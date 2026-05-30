package poker.round

import poker.turn.TurnResult
import poker.turn.TurnState
import poker.user.User

data class Round(
    val matchId: Int,
    val roundNumber: Int,
    val pot: Int,
    val players: List<User>,
    val turns: MutableList<TurnResult>,
    var winnerId: Int?,
    var currentTurnState: TurnState? = null,
    var currPlayerId: Int?,
)
