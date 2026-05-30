package poker.model

data class LobbySummaryOutputModel(
    val id: Int,
    val name: String,
    val hostUsername: String,
    val currentPlayerCount: Int,
    val expectedPlayers: Int,
    val state: String,
)
