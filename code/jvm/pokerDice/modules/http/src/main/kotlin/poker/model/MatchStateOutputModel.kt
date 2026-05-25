package poker.model

data class MatchStateOutputModel(
    val matchId: Int,
    val currentRound: Int,
    val totalRounds: Int,
    val status: String,
    val players: List<PlayerMatchOutputModel>,
    val rounds: List<RoundOutputModel>
)
