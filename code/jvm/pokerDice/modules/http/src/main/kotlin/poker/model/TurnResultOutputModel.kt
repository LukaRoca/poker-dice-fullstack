package poker.model

data class TurnResultOutputModel(
    val playerId: Int,
    val finalHand: String,
    val score: Int,
)
