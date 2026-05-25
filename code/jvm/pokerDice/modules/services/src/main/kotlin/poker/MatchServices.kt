package poker

import jakarta.inject.Named
import kotlinx.datetime.Clock
import poker.lobby.LobbyDomain
import poker.match.DiceAction
import poker.match.Match
import poker.match.MatchDomain
import poker.match.MatchState
import poker.match.MatchStatus
import poker.round.Round
import poker.round.RoundResult
import poker.turn.TurnResult
import poker.turn.TurnState
import poker.user.AuthenticatedUser
import poker.utils.Either
import transaction.TransactionManager
import java.util.concurrent.ConcurrentHashMap

sealed class MatchError {
    object MatchNotFound : MatchError()
    object NotLobbyHost : MatchError()
    object NotEnoughPlayers : MatchError()
    object MatchAlreadyStarted : MatchError()
    object RoundNotFound : MatchError()
    object NotYourTurn : MatchError()
    object LobbyNotFound : MatchError()
    object LobbyConditionsNotMet : MatchError()
    object RoundAlreadyEnded : MatchError() // Novo erro
    object RoundInProgress : MatchError() // Novo erro
}

@Named
class MatchServices(
    private val transactionManager: TransactionManager,
    private val matchDomain: MatchDomain,
    private val lobbyDomain: LobbyDomain,
    private val clock: Clock = Clock.System,
) {
    private val activeTurns = ConcurrentHashMap<Pair<Int, Int>, TurnState>()

    fun startMatch(
        user: AuthenticatedUser,
        lobbyId: Int,
    ): Either<MatchError, Match> {
        return transactionManager.run {
            val lobbyRepo = it.lobbyRepository
            val lobby = lobbyRepo.getLobbyById(lobbyId)
                ?: return@run Either.Left(MatchError.LobbyNotFound)

            if (lobby.host.id != user.user.id) {
                return@run Either.Left(MatchError.NotLobbyHost)
            }

            if (lobby.players.size < 2) {
                return@run Either.Left(MatchError.NotEnoughPlayers)
            }

            createAndPersistMatch(it, lobbyId)
        }
    }

    fun tryAutoStartMatch(lobbyId: Int): Either<MatchError, Match> {
        return transactionManager.run {
            val lobbyRepo = it.lobbyRepository
            val lobby = lobbyRepo.getLobbyById(lobbyId)
                ?: return@run Either.Left(MatchError.LobbyNotFound)

            if (!lobbyDomain.canStartMatch(lobby, clock.now())) {
                return@run Either.Left(MatchError.LobbyConditionsNotMet)
            }

            createAndPersistMatch(it, lobbyId)
        }
    }

    private fun createAndPersistMatch(
        transaction: transaction.Transaction,
        lobbyId: Int
    ): Either<MatchError, Match> {
        val lobbyRepo = transaction.lobbyRepository
        val matchRepo = transaction.matchRepository

        val lobby = lobbyRepo.getLobbyById(lobbyId)
            ?: return Either.Left(MatchError.LobbyNotFound)

        val existingMatch = matchRepo.getMatchByLobbyId(lobbyId)
        if (existingMatch != null && existingMatch.status != MatchStatus.NOT_STARTED) {
            return Either.Left(MatchError.MatchAlreadyStarted)
        }

        val domainMatch = lobbyDomain.startMatch(lobby)

        val createdMatch = matchRepo.createMatch(
            lobbyId = domainMatch.lobbyId,
            ante = domainMatch.ante,
            totalRounds = domainMatch.totalRounds
        )

        lobby.players.forEach { player ->
            matchRepo.addPlayerToMatch(createdMatch.matchId, player.id)
        }

        val matchWithPlayers = matchRepo.getMatchById(createdMatch.matchId)
            ?: return Either.Left(MatchError.MatchNotFound)

        val firstRound = matchDomain.startNewRound(matchWithPlayers)

        matchRepo.createRound(firstRound)
        matchRepo.updateMatch(matchWithPlayers)

        lobbyRepo.updateLobbyState(lobby.id.id, "CLOSED")

        return Either.Right(matchWithPlayers)
    }

    fun startNewRound(
        user: AuthenticatedUser,
        matchId: Int,
    ): Either<MatchError, Round> {
        return transactionManager.run {
            val matchRepo = it.matchRepository
            val lobbyRepo = it.lobbyRepository

            val match = matchRepo.getMatchById(matchId)
                ?: return@run Either.Left(MatchError.MatchNotFound)

            val lobby = lobbyRepo.getLobbyById(match.lobbyId)
                ?: return@run Either.Left(MatchError.LobbyNotFound)

            if (lobby.host.id != user.user.id) {
                return@run Either.Left(MatchError.NotLobbyHost)
            }

            tryAutoStartNewRoundInternal(it, match)
        }
    }

    fun listOngoingMatches(): Either<MatchError, List<Match>> {
        return transactionManager.run {
            val matchRepo = it.matchRepository
            val matches = matchRepo.listOngoingMatches()
            Either.Right(matches)
        }
    }

    fun tryAutoStartNewRound(
        matchId: Int,
    ): Either<MatchError, Round> {
        return transactionManager.run {
            val matchRepo = it.matchRepository
            val match = matchRepo.getMatchById(matchId)
                ?: return@run Either.Left(MatchError.MatchNotFound)

            tryAutoStartNewRoundInternal(it, match)
        }
    }

    private fun tryAutoStartNewRoundInternal(
        transaction: transaction.Transaction,
        match: Match
    ): Either<MatchError, Round> {
        val matchRepo = transaction.matchRepository

        if (match.status == MatchStatus.FINISHED) {
            return Either.Left(MatchError.MatchAlreadyStarted)
        }

        val lastRound = match.rounds.lastOrNull()
        if (lastRound != null && lastRound.currPlayerId != null) {
            return Either.Left(MatchError.RoundInProgress)
        }

        val newRound = matchDomain.startNewRound(match)
        matchRepo.createRound(newRound)
        matchRepo.updateMatch(match)

        return Either.Right(newRound)
    }

    fun playTurn(
        user: AuthenticatedUser,
        matchId: Int,
        roundNumber: Int,
        heldIndices: List<Int>?,
    ): Either<MatchError, TurnState> {
        return transactionManager.run {
            val matchRepo = it.matchRepository
            val match = matchRepo.getMatchById(matchId)
                ?: return@run Either.Left(MatchError.MatchNotFound)

            val round = match.rounds.find { r -> r.roundNumber == roundNumber }
                ?: return@run Either.Left(MatchError.RoundNotFound)

            val action = DiceAction(heldIndices ?: emptyList())
            val turnKey = Pair(matchId, roundNumber)
            val currentTurnState = activeTurns[turnKey]

            try {
                val newTurnState = matchDomain.playTurn(round, user.user.id, action, currentTurnState)
                activeTurns[turnKey] = newTurnState
                Either.Right(newTurnState)
            } catch (e: IllegalArgumentException) {
                Either.Left(MatchError.NotYourTurn)
            }
        }
    }

    fun endTurn(
        user: AuthenticatedUser,
        matchId: Int,
        roundNumber: Int,
    ): Either<MatchError, TurnResult> {
        return transactionManager.run {
            val matchRepo = it.matchRepository
            val match = matchRepo.getMatchById(matchId)
                ?: return@run Either.Left(MatchError.MatchNotFound)

            val round = match.rounds.find { r -> r.roundNumber == roundNumber }
                ?: return@run Either.Left(MatchError.RoundNotFound)

            val turnKey = Pair(matchId, roundNumber)
            val turnState = activeTurns[turnKey]
            if (turnState == null || turnState.playerId != user.user.id) {
                return@run Either.Left(MatchError.NotYourTurn)
            }

            val turnResult = matchDomain.endPlayerTurn(round, user.user.id, turnState)
            activeTurns.remove(turnKey)

            matchRepo.saveRound(round)
            val createdTurnId = matchRepo.createTurn(matchId, roundNumber, turnResult)

            Either.Right(turnResult.copy(turnId = createdTurnId))
        }
    }

    fun endRound(
        user: AuthenticatedUser,
        matchId: Int,
        roundNumber: Int,
    ): Either<MatchError, RoundResult> {
        return transactionManager.run {
            val lobbyRepo = it.lobbyRepository
            val match = it.matchRepository.getMatchById(matchId) ?: return@run Either.Left(MatchError.MatchNotFound)
            val lobby = lobbyRepo.getLobbyById(match.lobbyId) ?: return@run Either.Left(MatchError.LobbyNotFound)

            if (user.user.id != lobby.host.id) return@run Either.Left(MatchError.NotLobbyHost)

            tryAutoEndRoundInternal(it, matchId, roundNumber)
        }
    }

    fun tryAutoEndRound(
        matchId: Int,
        roundNumber: Int,
    ): Either<MatchError, RoundResult> {
        return transactionManager.run {
            tryAutoEndRoundInternal(it, matchId, roundNumber)
        }
    }

    private fun tryAutoEndRoundInternal(
        transaction: transaction.Transaction,
        matchId: Int,
        roundNumber: Int,
    ): Either<MatchError, RoundResult> {
        val matchRepo = transaction.matchRepository
        val userRepo = transaction.usersRepository

        val match = matchRepo.getMatchById(matchId) ?: return Either.Left(MatchError.MatchNotFound)
        val round = match.rounds.find { it.roundNumber == roundNumber } ?: return Either.Left(MatchError.RoundNotFound)

        val currentRound = match.rounds.lastOrNull { it.roundNumber == match.currentRound }
            ?: return Either.Left(MatchError.RoundNotFound)

        val roundPlayers = currentRound.players



        if (round.winnerId != null) {
            return Either.Left(MatchError.RoundAlreadyEnded)
        }
        if (round.currPlayerId != null) {
            return Either.Left(MatchError.RoundInProgress)
        }

        val result = matchDomain.endRound(match, round)

        matchRepo.saveRoundResult(matchId, result)
        matchRepo.updateMatch(match)

        roundPlayers.forEach { player ->
            val isWinner = (result.winnerId != null && player.id == result.winnerId)
            userRepo.updateUserStats(player.id, isWinner)
        }

        return Either.Right(result)
    }

    fun getMatchState(matchId: Int): Either<MatchError, MatchState> {
        return transactionManager.run {
            val matchRepo = it.matchRepository
            val match = matchRepo.getMatchById(matchId)
            if (match == null) {
                Either.Left(MatchError.MatchNotFound)
            } else {
                Either.Right(matchDomain.getMatchState(match))
            }
        }
    }
}