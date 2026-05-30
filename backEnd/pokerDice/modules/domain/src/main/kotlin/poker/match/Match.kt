package poker.match

import poker.round.Round
import poker.user.User

enum class MatchStatus {
    NOT_STARTED,
    ONGOING,
    FINISHED,
}

data class Match(
    val matchId: Int,
    val lobbyId: Int,
    var players: List<User>,
    var currentRound: Int = 0,
    val ante: Int,
    val totalRounds: Int,
    var status: MatchStatus = MatchStatus.NOT_STARTED,
    val rounds: MutableList<Round> = mutableListOf(),
)
