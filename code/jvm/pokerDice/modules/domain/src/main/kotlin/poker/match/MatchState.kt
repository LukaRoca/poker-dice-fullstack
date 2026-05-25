package poker.match

import poker.round.Round
import poker.user.User

data class MatchState(
    val matchId: Int,
    val currentRound: Int,
    val totalRounds: Int,
    val status: MatchStatus,
    val players: List<User>,
    val rounds: List<Round>,
)
