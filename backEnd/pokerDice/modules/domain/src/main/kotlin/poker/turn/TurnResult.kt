package poker.turn

data class TurnResult(
    val turnId: Int?,
    val playerId: Int,
    val finalHand: String,
    val score: Int,
)
