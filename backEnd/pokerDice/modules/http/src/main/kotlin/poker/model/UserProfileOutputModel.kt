package poker.model

data class UserProfileOutputModel(
    val id: Int,
    val name: String,
    val balance: Int,
    val roundsPlayed: Int,
    val roundsWon: Int,
    val winRate: Double
)
