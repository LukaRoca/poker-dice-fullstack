package poker.events

import kotlinx.datetime.Instant

sealed class LobbyEvent(
    val type: String,
) {
    data class PlayerJoined(
        val lobbyId: Int,
        val userId: Int,
        val username: String,
    ) : LobbyEvent("player_joined")

    data class PlayerLeft(
        val lobbyId: Int,
        val userId: Int,
        val username: String,
    ) : LobbyEvent("player_left")

    data class MatchStarted(
        val lobbyId: Int,
        val matchId: Int,
    ) : LobbyEvent("match_started")

    data class KeepAlive(
        val timestamp: Instant,
    ) : LobbyEvent("keep_alive")
}
