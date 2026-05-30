package poker.lobby

import kotlinx.datetime.Instant
import poker.user.User
import poker.utils.ExpectedPlayers
import poker.utils.Id
import poker.utils.Name
import kotlin.time.Duration

enum class LobbyStatus {
    OPEN,
    FULL,
    CLOSED,
}

data class Lobby(
    val id: Id,
    val name: Name,
    val host: User,
    val description: String,
    val rounds: Int,
    val players: List<User>,
    val expectedPlayers: ExpectedPlayers,
    val timeout: Duration,
    val createdAt: Instant,
    val ante: Int = 1,
    var status: LobbyStatus = LobbyStatus.OPEN,
) {
    init {
        require(expectedPlayers.count >= 2) { "Expected players must be at least 2" }
        require(players.size <= expectedPlayers.count) { "Max players exceeded" }
    }
}
