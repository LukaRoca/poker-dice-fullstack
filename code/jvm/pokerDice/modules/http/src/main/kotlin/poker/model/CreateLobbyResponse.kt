package poker.model

data class CreateLobbyResponse(
    val lobbyId: Int,
    val name: String,
    val hostUsername: String,
)
