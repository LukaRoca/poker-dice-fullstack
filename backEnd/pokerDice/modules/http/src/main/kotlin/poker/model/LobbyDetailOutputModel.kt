package poker.model

data class LobbyDetailOutputModel(
    val id: Int,
    val name: String,
    val description: String,
    val host: PlayerOutputModel,
    val rounds: Int,
    val ante: Int,
    val expectedPlayers: Int,
    val state: String,
    val players: List<PlayerOutputModel>,
    val matchId: Int? = null
)