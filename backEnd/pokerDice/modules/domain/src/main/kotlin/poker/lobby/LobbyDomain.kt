package poker.lobby

import org.springframework.stereotype.Component
import poker.match.Match
import poker.match.MatchStatus
import poker.user.User
import kotlinx.datetime.Instant

@Component
class LobbyDomain {
    fun canStartMatch(
        lobby: Lobby,
        now: Instant,
    ): Boolean {
        val minPlayersMet = lobby.players.size >= 2
        val fullLobby = lobby.players.size == lobby.expectedPlayers.count
        val deadline = lobby.createdAt + lobby.timeout
        val timeoutElapsed = now >= deadline
        return minPlayersMet && (fullLobby || timeoutElapsed)
    }

    fun addPlayer(
        lobby: Lobby,
        player: User,
    ): Lobby {
        require(lobby.status == LobbyStatus.OPEN) { "Não é possível entrar em um lobby fechado" }
        require(lobby.players.size < lobby.expectedPlayers.count) { "Lobby já está cheio" }
        return lobby.copy(players = lobby.players + player)
    }

    fun removePlayer(
        lobby: Lobby,
        playerId: Int,
    ): Lobby {
        return lobby.copy(players = lobby.players.filterNot { it.id == playerId })
    }

    fun deleteLobby(
        lobby: Lobby,
        userId: Int,
    ): Lobby {
        require(lobby.host.id == userId) { "Apenas o host pode apagar o lobby" }
        return lobby.copy(
            status = LobbyStatus.CLOSED,
            players = emptyList(),
        )
    }

    fun isPlayerInLobby(
        lobby: Lobby,
        playerId: Int,
    ): Boolean {
        return lobby.players.any { it.id == playerId }
    }

    fun startMatch(lobby: Lobby): Match {
        return Match(
            matchId = 0,
            lobbyId = lobby.id.id,
            players = lobby.players,
            currentRound = 0,
            ante = lobby.ante,
            totalRounds = lobby.rounds,
            status = MatchStatus.NOT_STARTED,
            rounds = mutableListOf(),
        )
    }
}
