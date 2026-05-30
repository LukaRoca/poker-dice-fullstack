package poker.jdbi

import kotlinx.datetime.toKotlinInstant
import org.jdbi.v3.core.Handle
import poker.PasswordValidationInfo
import poker.lobby.Lobby
import poker.lobby.LobbyStatus
import poker.user.User
import poker.utils.ExpectedPlayers
import poker.utils.Id
import poker.utils.Name
import transaction.repository.LobbyRepository
import kotlin.time.Duration.Companion.milliseconds
import java.time.Instant as JavaInstant

class JdbiLobbyRepository(
    private val handle: Handle,
) : LobbyRepository {

    override fun createLobby(
        name: String,
        isPublic: Boolean,
        hostId: Int,
        rounds: Int,
        expectedPlayers: Int,
        ante: Int,
        timeout: Long,
    ): Lobby {
        val lobbyId = handle.createUpdate(
            """
            INSERT INTO dbo.LOBBY (name, description, hostId, rounds, expectedPlayers, ante, state, timeout)
            VALUES (:name, :description, :hostId, :rounds, :expectedPlayers, :ante, 'OPEN', :timeout)
            """
        )
            .bind("name", name)
            .bind("description", if (isPublic) "public" else "private")
            .bind("hostId", hostId)
            .bind("rounds", rounds)
            .bind("expectedPlayers", expectedPlayers)
            .bind("ante", ante)
            .bind("timeout", timeout)
            .executeAndReturnGeneratedKeys("id")
            .mapTo(Int::class.java)
            .one()

        handle.createUpdate(
            "INSERT INTO dbo.LOBBY_PLAYER (lobbyId, userId) VALUES (:lobbyId, :hostId)"
        )
            .bind("lobbyId", lobbyId)
            .bind("hostId", hostId)
            .execute()

        return getLobbyById(lobbyId) ?: error("Failed to load lobby after creation")
    }

    override fun getLobbyById(id: Int): Lobby? {
        val lobbyRow: LobbyRow = handle.createQuery(
            """
            SELECT id, name, description, hostId, rounds, expectedPlayers, ante, state, 
                   COALESCE(timeout, 0) AS timeout, created_at
            FROM dbo.LOBBY
            WHERE id = :id
            """
        )
            .bind("id", id)
            .map { rs, _, _ ->
                LobbyRow(
                    id = rs.getInt("id"),
                    name = rs.getString("name"),
                    description = rs.getString("description"),
                    hostId = rs.getInt("hostId"),
                    rounds = rs.getInt("rounds"),
                    expectedPlayers = rs.getInt("expectedPlayers"),
                    ante = rs.getInt("ante"),
                    state = rs.getString("state"),
                    timeout = rs.getLong("timeout"),
                    createdAt = rs.getTimestamp("created_at").toInstant()
                )
            }.singleOrNull() ?: return null

        // CORREÇÃO 1: Adicionar colunas rounds_played e rounds_won ao SELECT
        val hostUser = handle.createQuery(
            "SELECT id, name, password_validation, balance, rounds_played, rounds_won FROM dbo.USER WHERE id = :hostId"
        )
            .bind("hostId", lobbyRow.hostId)
            .map { rs, _, _ ->
                User(
                    id = rs.getInt("id"),
                    name = rs.getString("name"),
                    password = PasswordValidationInfo(rs.getString("password_validation")),
                    balance = rs.getInt("balance"),
                    roundsPlayed = rs.getInt("rounds_played"), // Ler coluna
                    roundsWon = rs.getInt("rounds_won")        // Ler coluna
                )
            }.one()

        // CORREÇÃO 2: Adicionar colunas rounds_played e rounds_won ao SELECT
        val players: List<User> = handle.createQuery(
            """
            SELECT u.id, u.name, u.password_validation, u.balance, u.rounds_played, u.rounds_won
            FROM dbo.LOBBY_PLAYER lp 
            JOIN dbo.USER u ON u.id = lp.userId 
            WHERE lp.lobbyId = :lobbyId
            """
        )
            .bind("lobbyId", id)
            .map { rs, _, _ ->
                User(
                    id = rs.getInt("id"),
                    name = rs.getString("name"),
                    password = PasswordValidationInfo(rs.getString("password_validation")),
                    balance = rs.getInt("balance"),
                    roundsPlayed = rs.getInt("rounds_played"), // Ler coluna
                    roundsWon = rs.getInt("rounds_won")        // Ler coluna
                )
            }.list()

        return Lobby(
            id = Id(lobbyRow.id),
            name = Name(lobbyRow.name),
            host = hostUser,
            description = lobbyRow.description ?: "",
            rounds = lobbyRow.rounds,
            players = players,
            expectedPlayers = ExpectedPlayers(lobbyRow.expectedPlayers),
            timeout = lobbyRow.timeout.milliseconds,
            ante = lobbyRow.ante,
            status = LobbyStatus.valueOf(lobbyRow.state),
            createdAt = lobbyRow.createdAt.toKotlinInstant(),
        )
    }

    override fun listAvailableLobbies(): List<Lobby> {
        return handle.createQuery("""
            SELECT id FROM dbo.LOBBY 
            WHERE state IN ('OPEN', 'FULL')
        """).mapTo(Int::class.java)
            .list()
            .mapNotNull { getLobbyById(it) }
            .filter { it.status == LobbyStatus.OPEN || it.status == LobbyStatus.FULL }
    }

    override fun addPlayerToLobby(lobbyId: Int, userId: Int) {
        handle.createUpdate(
            "INSERT INTO dbo.LOBBY_PLAYER (lobbyId, userId) VALUES (:lobbyId, :userId) ON CONFLICT DO NOTHING"
        )
            .bind("lobbyId", lobbyId)
            .bind("userId", userId)
            .execute()

        handle.inTransaction<Unit, Exception> { h -> updateLobbyFullState(h, lobbyId) }
    }

    override fun removePlayerFromLobby(lobbyId: Int, userId: Int) {
        handle.inTransaction<Unit, Exception> { h ->
            h.createUpdate("DELETE FROM dbo.LOBBY_PLAYER WHERE lobbyId = :lobbyId AND userId = :userId")
                .bind("lobbyId", lobbyId)
                .bind("userId", userId)
                .execute()

            updateLobbyFullState(h, lobbyId)
        }
    }

    override fun getLobbyByName(name: String): Lobby? {
        val id = handle.createQuery("SELECT id FROM dbo.LOBBY WHERE name = :name")
            .bind("name", name)
            .mapTo(Int::class.java)
            .singleOrNull()
        return id?.let { getLobbyById(it) }
    }

    private fun updateLobbyFullState(h: Handle, lobbyId: Int) {
        val playerCount = h.createQuery("SELECT COUNT(*) FROM dbo.LOBBY_PLAYER WHERE lobbyId = :lobbyId")
            .bind("lobbyId", lobbyId)
            .mapTo(Int::class.java)
            .one()

        val lobbyRow = h.createQuery("SELECT expectedPlayers, state FROM dbo.LOBBY WHERE id = :lobbyId")
            .bind("lobbyId", lobbyId)
            .map { rs, _ -> Pair(rs.getInt("expectedPlayers"), rs.getString("state")) }
            .singleOrNull() ?: return

        val expectedPlayers = lobbyRow.first
        val currentState = lobbyRow.second

        if (currentState == "CLOSED") return

        val newState = when {
            playerCount == 0 -> "CLOSED"
            playerCount >= expectedPlayers -> "FULL"
            else -> "OPEN"
        }

        if (newState != currentState) {
            updateLobbyState(lobbyId, newState)
        }

        if (playerCount == 0) {
            deleteLobby(lobbyId)
        }
    }

    override fun updateLobbyState(lobbyId: Int, state: String) {
        handle.createUpdate("UPDATE dbo.LOBBY SET state = :state WHERE id = :lobbyId")
            .bind("state", state)
            .bind("lobbyId", lobbyId)
            .execute()
    }

    override fun deleteLobby(lobbyId: Int) {
        handle.inTransaction<Unit, Exception> { h ->
            h.createUpdate("DELETE FROM dbo.LOBBY_PLAYER WHERE lobbyId = :lobbyId")
                .bind("lobbyId", lobbyId)
                .execute()

            h.createUpdate("DELETE FROM dbo.LOBBY WHERE id = :lobbyId")
                .bind("lobbyId", lobbyId)
                .execute()
        }
    }

    data class LobbyRow(
        val id: Int,
        val name: String,
        val description: String?,
        val hostId: Int,
        val rounds: Int,
        val expectedPlayers: Int,
        val ante: Int,
        val state: String,
        val timeout: Long,
        val createdAt: JavaInstant
    )
}