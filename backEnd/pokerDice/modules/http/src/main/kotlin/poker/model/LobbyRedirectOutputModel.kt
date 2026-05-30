package poker.model

data class LobbyRedirectOutputModel(
    val lobbyId: Int,
    val matchId: Int,
    val redirect: Boolean,
    val message: String
)