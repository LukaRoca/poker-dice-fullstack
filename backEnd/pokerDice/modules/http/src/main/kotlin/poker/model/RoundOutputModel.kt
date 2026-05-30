package poker.model

data class RoundOutputModel(
    val roundNumber: Int,
    val pot: Int,
    val winnerId: Int?,
    val currentPlayerId: Int?,
    val turns: List<TurnResultOutputModel>,
)
