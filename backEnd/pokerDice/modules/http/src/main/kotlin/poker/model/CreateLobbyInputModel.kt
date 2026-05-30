package poker.model

data class CreateLobbyInputModel(
    val name: String,
    val isPublic: Boolean,
    val rounds: Int,
    val expectedPlayers: Int,
    val ante: Int,
    val timeout: Long,
)
