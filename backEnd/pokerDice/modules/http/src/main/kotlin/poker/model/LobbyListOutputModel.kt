package poker.model

data class LobbyListOutputModel(
    val items: List<LobbySummaryOutputModel>,
    val total: Int,
    val page : Int ,
    val pageSize : Int ,
    val totalPages : Int
)
