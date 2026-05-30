package poker.match

import org.springframework.stereotype.Component
import poker.Face
import poker.Hand
import poker.round.Round
import poker.round.RoundResult
import poker.turn.TurnResult
import poker.turn.TurnState
import kotlin.random.Random

data class DiceAction(
    val heldIndices: List<Int>,
)

@Component
class MatchDomain {
    private val faceMap by lazy { Face.values().associateBy { it.symbol } }

    private fun stringToHand(handString: String): Hand? {
        val faces = handString.split(" ").mapNotNull { symbol -> faceMap[symbol] }
        if (faces.size != 5) return null
        return Hand(faces)
    }

    fun isMatchFinished(match: Match): Boolean =
        match.currentRound >= match.totalRounds || match.players.count { it.balance >= match.ante } <= 1

    fun startNewRound(match: Match): Round {
        if (match.status == MatchStatus.NOT_STARTED) {
            match.status = MatchStatus.ONGOING
        }

        val hostId = match.players.firstOrNull()?.id ?: 0
        val eligiblePlayers = match.players.filter { it.balance >= match.ante }

        if (eligiblePlayers.size < 2) {
            match.status = MatchStatus.FINISHED
            throw IllegalStateException("Match cannot continue: less than two players can afford the ante.")
        }

        val initialStartPlayerId =
            if (match.currentRound == 0) {
                eligiblePlayers.minOfOrNull { it.id } ?: hostId
            } else {
                val previousRound = match.rounds.last()
                val previousPlayerOrder = previousRound.players
                val previousStartPlayerId = previousPlayerOrder.firstOrNull()?.id ?: hostId

                val previousStartIndex = eligiblePlayers.indexOfFirst { it.id == previousStartPlayerId }

                val nextStartIndex =
                    if (previousStartIndex != -1) {
                        (previousStartIndex + 1) % eligiblePlayers.size
                    } else {
                        0
                    }
                eligiblePlayers[nextStartIndex].id
            }

        val sortedPlayersForRound = rotatePlayers(eligiblePlayers, initialStartPlayerId)

        val pot = sortedPlayersForRound.size * match.ante
        sortedPlayersForRound.forEach { it.balance -= match.ante }

        val newRound =
            Round(
                matchId = match.matchId,
                roundNumber = match.currentRound + 1,
                pot = pot,
                players = sortedPlayersForRound,
                turns = mutableListOf(),
                winnerId = null,
                currPlayerId = sortedPlayersForRound.first().id,
            )

        match.rounds.add(newRound)
        match.currentRound += 1

        return newRound
    }

    private fun rotatePlayers(players: List<poker.user.User>, startId: Int): List<poker.user.User> {
        val index = players.indexOfFirst { it.id == startId }
        if (index == -1) return players
        return players.drop(index) + players.take(index)
    }

    fun isPlayerTurn(
        round: Round,
        playerId: Int,
    ): Boolean {
        return round.currPlayerId == playerId
    }

    fun getMatchState(match: Match): MatchState =
        MatchState(
            matchId = match.matchId,
            currentRound = match.currentRound,
            totalRounds = match.totalRounds,
            status = match.status,
            players = match.players,
            rounds = match.rounds,
        )

    fun playTurn(
        round: Round,
        playerId: Int,
        action: DiceAction,
        currentTurnState: TurnState?,
    ): TurnState {
        require(isPlayerTurn(round, playerId)) { "Não é a vez do jogador" }

        if (currentTurnState == null) {
            require(action.heldIndices.isEmpty()) {"Na primeira rolagem não há dados para manter"}
            return TurnState(
                playerId = playerId,
                currentDice = List(5) { rollDie() },
                rollsLeft = 2,
            )
        }

        require(currentTurnState.rollsLeft>0) {"Não tens mais rolagens"}

        val newDice = currentTurnState.currentDice.mapIndexed { idx, face ->
            if (action.heldIndices.contains(idx)) face else rollDie()
        }
        val newRollsLeft = currentTurnState.rollsLeft - 1

        return currentTurnState.copy(
            currentDice = newDice,
            rollsLeft = newRollsLeft
        )
    }

    fun endPlayerTurn(
        round: Round,
        playerId: Int,
        turnState: TurnState?,
    ): TurnResult {
        require(turnState != null) { "Nenhum turno em progresso para finalizar." }
        require(turnState.playerId == playerId) { "Não pode finalizar o turno de outro jogador." }

        val finalHand = Hand(turnState.currentDice)
        val score = finalHand.rank().strength
        val turnResult =
            TurnResult(
                turnId = null,
                playerId = playerId,
                finalHand = turnState.currentDice.joinToString(" ") { it.symbol },
                score = score,
            )

        round.turns.add(turnResult)

        val turnsCount = round.turns.size
        val totalPlayers = round.players.size

        if (turnsCount >= totalPlayers) {
            round.currPlayerId = null
        } else {
            val playersWhoPlayedIds = round.turns.map { it.playerId }.toSet()
            val nextPlayer = round.players.firstOrNull { !playersWhoPlayedIds.contains(it.id) }
            round.currPlayerId = nextPlayer?.id
        }
        return turnResult
    }
    fun endRound(
        match: Match,
        round: Round,
    ): RoundResult {
        val turns = round.turns
        if (turns.isEmpty()) {
            return RoundResult(roundNumber = round.roundNumber, winnerId = null, pot = round.pot)
        }

        val winnerTurn =
            turns.mapNotNull { turn -> stringToHand(turn.finalHand)?.let { hand -> Pair(turn, hand) } }
                .maxWithOrNull(compareBy { it.second })?.first

        val winnerId = winnerTurn?.playerId

        if (winnerId != null) {
            val winnerUser = match.players.find { it.id == winnerId }
            winnerUser?.let {
                it.balance += round.pot
            }
            round.winnerId = winnerId
        }

        val remainingPlayers = match.players.filter { it.balance >= match.ante }
        match.players = remainingPlayers

        val shouldFinish = match.currentRound >= match.totalRounds || remainingPlayers.size <= 1
        if (shouldFinish) {
            match.status = MatchStatus.FINISHED
        }

        return RoundResult(roundNumber = round.roundNumber, winnerId = winnerId, pot = round.pot)
    }

    private fun rollDie(): Face {
        val faces = Face.values()
        return faces[Random.nextInt(faces.size)]
    }
}