package poker.events

import kotlinx.datetime.Instant

sealed class GlobalEvent(
    val type: String,
) {
    data class InviteReceived(
        val fromUsername: String,
        val lobbyId: Int,
        val lobbyName: String,
    ) : GlobalEvent("invite_received")

    data class MatchStarted(
        val matchId: Int,
    ) : GlobalEvent("match_started")

    data class KeepAlive(
        val timestamp: Instant,
    ) : GlobalEvent("keep_alive")
}
