package poker.round

data class RoundResult(
    val roundNumber: Int,
    val winnerId: Int?,
    val pot: Int,
)
