package poker.turn

import poker.Face

data class TurnState(
    val playerId: Int,
    val currentDice: List<Face>,
    val rollsLeft: Int,
)
