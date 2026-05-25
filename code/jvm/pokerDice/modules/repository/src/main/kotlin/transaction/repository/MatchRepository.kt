package transaction.repository

import poker.match.Match
import poker.round.Round
import poker.round.RoundResult
import poker.turn.TurnResult

interface MatchRepository {
    fun createMatch(
        lobbyId: Int,
        ante: Int,
        totalRounds: Int,
    ): Match

    fun getMatchById(id: Int): Match?

    fun getMatchByLobbyId(lobbyId: Int): Match?

    fun updateStatus(
        matchId: Int,
        status: String,
    )

    fun updateMatch(match: Match)

    fun addPlayerToMatch(
        matchId: Int,
        playerId: Int,
    )

    fun removePlayerFromMatch(
        matchId: Int,
        playerId: Int,
    )

    fun createRound(round: Round): Round

    fun getRoundByNumberAndMatch(
        matchId: Int,
        roundNumber: Int,
    ): Round?

    fun getRoundsByMatch(matchId: Int): List<Round>

    fun setRoundWinner(
        matchId: Int,
        roundNumber: Int,
        playerId: Int?,
    )

    fun saveRound(round: Round)

    fun saveRoundResult(
        matchId: Int,
        result: RoundResult,
    )

    fun createTurn(
        matchId: Int,
        roundNumber: Int,
        turn: TurnResult,
    ): Int

    fun saveTurn(turn: TurnResult)

    fun getTurnsByRound(
        matchId: Int,
        roundNumber: Int,
    ): List<TurnResult>

    fun listOngoingMatches(): List<Match>
}