package poker.jdbi

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import poker.PasswordValidationInfo
import poker.match.Match
import poker.match.MatchStatus
import poker.round.Round
import poker.round.RoundResult
import poker.turn.TurnResult
import poker.user.User
import transaction.repository.MatchRepository

class JdbiMatchRepository(
    private val handle: Handle,
) : MatchRepository {

    override fun createMatch(
        lobbyId: Int,
        ante: Int,
        totalRounds: Int,
    ): Match {
        val matchId = handle.createUpdate(
            """
            INSERT INTO dbo.MATCH (lobbyid, currentround, ante, totalrounds, status)
            VALUES (:lobbyId, 0, :ante, :totalRounds, :status)
            """
        )
            .bind("lobbyId", lobbyId)
            .bind("ante", ante)
            .bind("totalRounds", totalRounds)
            .bind("status", MatchStatus.NOT_STARTED.name)
            .executeAndReturnGeneratedKeys("matchid")
            .mapTo(Int::class.java)
            .one()

        return getMatchById(matchId) ?: error("Failed to fetch match immediately after creation")
    }

    override fun getMatchById(matchId: Int): Match? {
        val match = handle.createQuery(
            """
            SELECT matchid, lobbyid, currentround, ante, totalrounds, status
            FROM dbo.MATCH
            WHERE matchid = :matchId
            """
        )
            .bind("matchId", matchId)
            .map { rs, _, _ ->
                Match(
                    matchId = rs.getInt("matchid"),
                    lobbyId = rs.getInt("lobbyid"),
                    players = emptyList(),
                    currentRound = rs.getInt("currentround"),
                    ante = rs.getInt("ante"),
                    totalRounds = rs.getInt("totalRounds"),
                    status = MatchStatus.valueOf(rs.getString("status")),
                    rounds = mutableListOf()
                )
            }.singleOrNull()

        if (match != null) {
            val players = getMatchPlayers(match.matchId)
            val rounds = getRoundsByMatch(match.matchId, players)
            return match.copy(
                players = players,
                rounds = rounds.toMutableList()
            )
        }

        return null
    }

    override fun listOngoingMatches(): List<Match> {
        val ongoingMatchIds = handle.createQuery(
            """
            SELECT matchid FROM dbo.MATCH 
            WHERE status = :status
            """
        )
            .bind("status", MatchStatus.ONGOING.name)
            .mapTo(Int::class.java)
            .list()

        return ongoingMatchIds.mapNotNull { getMatchById(it) }
    }

    private fun getMatchPlayers(matchId: Int): List<User> {
        return handle.createQuery(
            // CORREÇÃO: Adicionar rounds_played e rounds_won ao SELECT
            """
            SELECT u.id, u.name, u.password_validation, u.balance, u.rounds_played, u.rounds_won
            FROM dbo.USER u
            INNER JOIN dbo.MATCH_PLAYER mp ON mp.user_id = u.id
            WHERE mp.matchid = :matchId
            ORDER BY u.id
            """
        )
            .bind("matchId", matchId)
            .map(UserRowMapper())
            .list()
    }

    override fun getMatchByLobbyId(lobbyId: Int): Match? {
        val matchId = handle.createQuery("SELECT matchid FROM dbo.MATCH WHERE lobbyid = :lobbyId")
            .bind("lobbyId", lobbyId)
            .mapTo(Int::class.java)
            .singleOrNull()

        return matchId?.let { getMatchById(it) }
    }

    override fun updateStatus(
        matchId: Int,
        status: String,
    ) {
        handle.createUpdate(
            """
            UPDATE dbo.MATCH
            SET status = :status
            WHERE matchid = :matchId
            """
        )
            .bind("status", status)
            .bind("matchId", matchId)
            .execute()
    }

    override fun updateMatch(match: Match) {
        handle.createUpdate(
            """
            UPDATE dbo.MATCH
            SET currentround = :currentRound, status = :status
            WHERE matchid = :matchId
            """
        )
            .bind("currentRound", match.currentRound)
            .bind("status", match.status.name)
            .bind("matchId", match.matchId)
            .execute()

        match.players.forEach { player ->
            handle.createUpdate("UPDATE dbo.USER SET balance = :balance WHERE id = :userId")
                .bind("balance", player.balance)
                .bind("userId", player.id)
                .execute()
        }
    }

    override fun addPlayerToMatch(
        matchId: Int,
        playerId: Int,
    ) {
        handle.createUpdate(
            """
            INSERT INTO dbo.MATCH_PLAYER (matchid, user_id)
            VALUES (:matchId, :playerId)
            ON CONFLICT DO NOTHING
            """
        )
            .bind("matchId", matchId)
            .bind("playerId", playerId)
            .execute()
    }

    override fun removePlayerFromMatch(
        matchId: Int,
        playerId: Int,
    ) {
        handle.createUpdate(
            """
            DELETE FROM dbo.MATCH_PLAYER
            WHERE matchid = :matchId AND user_id = :playerId
            """
        )
            .bind("matchId", matchId)
            .bind("playerId", playerId)
            .execute()
    }

    override fun createRound(round: Round): Round {
        handle.createUpdate(
            """
            INSERT INTO dbo.ROUND (matchid, round_number, pot, winner_id, curr_player_id)
            VALUES (:matchId, :roundNumber, :pot, :winnerId, :currPlayerId)
            """
        )
            .bind("matchId", round.matchId)
            .bind("roundNumber", round.roundNumber)
            .bind("pot", round.pot)
            .bind("winnerId", round.winnerId)
            .bind("currPlayerId", round.currPlayerId)
            .execute()

        return getRoundByNumberAndMatch(round.matchId, round.roundNumber)
            ?: error("Falha ao buscar a ronda imediatamente após a criação")
    }

    override fun getRoundByNumberAndMatch(
        matchId: Int,
        roundNumber: Int,
    ): Round? {
        val round = handle.createQuery(
            """
            SELECT matchid, round_number, pot, winner_id, curr_player_id
            FROM dbo.ROUND
            WHERE matchid = :matchId AND round_number = :roundNumber
            """
        )
            .bind("matchId", matchId)
            .bind("roundNumber", roundNumber)
            .map(RoundRowMapper())
            .singleOrNull() ?: return null

        val players = getMatchPlayers(matchId)
        val turns = getTurnsByRound(matchId, roundNumber)

        return round.copy(
            players = players,
            turns = turns.toMutableList()
        )
    }

    override fun getRoundsByMatch(matchId: Int): List<Round> {
        val playersForMatch = getMatchPlayers(matchId)
        return getRoundsByMatch(matchId, playersForMatch)
    }

    private fun getRoundsByMatch(
        matchId: Int,
        playersForMatch: List<User>,
    ): List<Round> {
        val baseRounds = handle.createQuery(
            """
            SELECT matchid, round_number, pot, winner_id, curr_player_id
            FROM dbo.ROUND
            WHERE matchid = :matchId
            ORDER BY round_number
            """
        )
            .bind("matchId", matchId)
            .map(RoundRowMapper())
            .list()

        return baseRounds.map { round ->
            val turns = getTurnsByRound(matchId, round.roundNumber)
            round.copy(
                players = playersForMatch,
                turns = turns.toMutableList()
            )
        }
    }

    override fun setRoundWinner(
        matchId: Int,
        roundNumber: Int,
        playerId: Int?,
    ) {
        handle.createUpdate(
            """
            UPDATE dbo.ROUND
            SET winner_id = :playerId
            WHERE matchid = :matchId AND round_number = :roundNumber
            """
        )
            .bind("playerId", playerId)
            .bind("matchId", matchId)
            .bind("roundNumber", roundNumber)
            .execute()
    }

    override fun saveRound(round: Round) {
        handle.createUpdate(
            """
            UPDATE dbo.ROUND
            SET
                pot = :pot,
                winner_id = :winnerId,
                curr_player_id = :currPlayerId
            WHERE matchid = :matchId AND round_number = :roundNumber
            """
        )
            .bind("pot", round.pot)
            .bind("winnerId", round.winnerId)
            .bind("currPlayerId", round.currPlayerId)
            .bind("matchId", round.matchId)
            .bind("roundNumber", round.roundNumber)
            .execute()
    }

    override fun saveRoundResult(
        matchId: Int,
        result: RoundResult,
    ) {
        handle.createUpdate(
            """
            UPDATE dbo.ROUND
            SET
                pot = :pot,
                winner_id = :winnerId
            WHERE matchid = :matchId AND round_number = :roundNumber
            """
        )
            .bind("pot", result.pot)
            .bind("winnerId", result.winnerId)
            .bind("matchId", matchId)
            .bind("roundNumber", result.roundNumber)
            .execute()
    }

    override fun createTurn(
        matchId: Int,
        roundNumber: Int,
        turn: TurnResult,
    ): Int {
        return handle.createUpdate(
            """
            INSERT INTO dbo.TURN (matchid, round_number, user_id, final_hand, hand_rank)
            VALUES (:matchId, :roundNumber, :playerId, :finalHand, :handRank)
            """
        )
            .bind("matchId", matchId)
            .bind("roundNumber", roundNumber)
            .bind("playerId", turn.playerId)
            .bind("finalHand", turn.finalHand)
            .bind("handRank", turn.score.toString())
            .executeAndReturnGeneratedKeys("id")
            .mapTo(Int::class.java)
            .one()
    }

    override fun getTurnsByRound(
        matchId: Int,
        roundNumber: Int,
    ): List<TurnResult> {
        return handle.createQuery(
            """
            SELECT id, matchid, user_id, final_hand, hand_rank
            FROM dbo.TURN
            WHERE matchid = :matchId AND round_number = :roundNumber
            ORDER BY id
            """
        )
            .bind("matchId", matchId)
            .bind("roundNumber", roundNumber)
            .map { rs, _ ->
                TurnResult(
                    turnId = rs.getInt("id"),
                    playerId = rs.getInt("user_id"),
                    finalHand = rs.getString("final_hand") ?: "",
                    score = rs.getString("hand_rank")?.toIntOrNull() ?: 0
                )
            }.list()
    }

    override fun saveTurn(turn: TurnResult) {
        val turnId = turn.turnId ?: error("Não é possível guardar um turno sem um turnId válido.")
        handle.createUpdate(
            """
            UPDATE dbo.TURN
            SET final_hand = :finalHand, hand_rank = :handRank
            WHERE id = :id
            """
        )
            .bind("finalHand", turn.finalHand)
            .bind("handRank", turn.score.toString())
            .bind("id", turnId)
            .execute()
    }

    private class UserRowMapper : RowMapper<User> {
        override fun map(rs: java.sql.ResultSet, ctx: StatementContext): User =
            User(
                id = rs.getInt("id"),
                name = rs.getString("name"),
                password = PasswordValidationInfo(rs.getString("password_validation")),
                balance = rs.getInt("balance"),
                // CORREÇÃO: Mapear novas colunas
                roundsPlayed = rs.getInt("rounds_played"),
                roundsWon = rs.getInt("rounds_won")
            )
    }

    private class RoundRowMapper : RowMapper<Round> {
        override fun map(rs: java.sql.ResultSet, ctx: StatementContext): Round =
            Round(
                matchId = rs.getInt("matchid"),
                roundNumber = rs.getInt("round_number"),
                pot = rs.getInt("pot"),
                players = emptyList(),
                turns = mutableListOf(),
                winnerId = rs.getObject("winner_id") as? Int,
                currPlayerId = rs.getObject("curr_player_id") as? Int,
            )
    }
}