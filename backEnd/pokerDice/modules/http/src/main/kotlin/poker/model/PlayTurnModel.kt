package poker.model

import poker.match.DiceAction

data class PlayTurnModel(
    val matchId: Int,
    val playerId: Int,
    val action: DiceAction,
)
