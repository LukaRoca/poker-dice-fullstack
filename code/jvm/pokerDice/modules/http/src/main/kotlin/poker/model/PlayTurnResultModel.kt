package poker.model

data class PlayTurnResultModel(
    val playerId: Int,
    val dice: List<String>,
    val rollsLeft: Int,
)
