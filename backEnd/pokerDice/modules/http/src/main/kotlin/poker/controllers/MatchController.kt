package poker.controllers

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import poker.MatchError
import poker.MatchServices
import poker.Routes
import poker.model.EndRoundModel
import poker.model.EndTurnOutputModel
import poker.model.MatchStateOutputModel
import poker.model.PlayTurnResultModel
import poker.model.PlayerActionModel
import poker.model.PlayerMatchOutputModel
import poker.model.Problem
import poker.model.RoundOutputModel
import poker.model.TurnResultOutputModel
import poker.user.AuthenticatedUser
import poker.user.User
import poker.utils.Either

@RestController
class MatchController(
    private val matchService: MatchServices,
) {
    @PostMapping(Routes.MATCH.PLAY)
    fun playTurn(
        @PathVariable matchId: Int,
        @PathVariable roundNumber: Int,
        user: AuthenticatedUser,
        @RequestBody(required = false) actionModel: PlayerActionModel?,
    ): ResponseEntity<*> {
        val heldIndices = actionModel?.heldIndices

        return when (val result = matchService.playTurn(user, matchId, roundNumber, heldIndices)) {
            is Either.Left ->
                when (result.value) {
                    MatchError.NotYourTurn -> Problem.response(403, Problem.notYourTurn)
                    MatchError.MatchNotFound -> Problem.response(404, Problem.matchNotFound)
                    MatchError.RoundNotFound -> Problem.response(404, Problem.roundNotFound)
                    else -> Problem.response(400, Problem.invalidRequestContent)
                }
            is Either.Right -> {
                val turnState = result.value
                val responseModel =
                    PlayTurnResultModel(
                        playerId = turnState.playerId,
                        dice = turnState.currentDice.map { it.symbol },
                        rollsLeft = turnState.rollsLeft,
                    )
                ResponseEntity.ok(responseModel)
            }
        }
    }

    @PostMapping(Routes.MATCH.END_TURN)
    fun endTurn(
        @PathVariable matchId: Int,
        @PathVariable roundNumber: Int,
        user: AuthenticatedUser,
    ): ResponseEntity<*> {
        val res = matchService.endTurn(user, matchId, roundNumber)
        return when (res) {
            is Either.Right -> ResponseEntity.ok(EndTurnOutputModel(matchId = matchId,
                playerId = res.value.playerId,
                finalHand = res.value.finalHand))
            is Either.Left ->
                when (res.value) {
                    MatchError.RoundNotFound -> Problem.response(404, Problem.roundNotFound)
                    MatchError.NotYourTurn -> Problem.response(403, Problem.notYourTurn)
                    else -> Problem.response(400, Problem.invalidRequestContent)
                }
        }
    }

    @GetMapping(Routes.MATCH.RESULT)
    fun getMatchState(
        @PathVariable matchId: Int,
    ): ResponseEntity<*> {
        val stateResult = matchService.getMatchState(matchId)

        return when (stateResult) {
            is Either.Left ->
                when (stateResult.value) {
                    MatchError.MatchNotFound -> Problem.response(404, Problem.matchNotFound)
                    else -> Problem.response(400, Problem.invalidRequestContent)
                }

            is Either.Right -> {
                val matchStateDomain = stateResult.value

                val matchStateOutput =
                    MatchStateOutputModel(
                        matchId = matchStateDomain.matchId,
                        currentRound = matchStateDomain.currentRound,
                        totalRounds = matchStateDomain.totalRounds,
                        status = matchStateDomain.status.toString(),
                        players = matchStateDomain.players.map { it.toPlayerMatchOutputModel() },
                        rounds =
                            matchStateDomain.rounds.map { round ->
                                RoundOutputModel(
                                    roundNumber = round.roundNumber,
                                    pot = round.pot,
                                    winnerId = round.winnerId,
                                    currentPlayerId = round.currPlayerId,
                                    turns =
                                        round.turns.map { turn ->
                                            TurnResultOutputModel(
                                                playerId = turn.playerId,
                                                finalHand = turn.finalHand,
                                                score = turn.score,
                                            )
                                        },
                                )
                            },
                    )

                ResponseEntity.ok(matchStateOutput)
            }
        }
    }

    @PostMapping(Routes.MATCH.END)
    fun endRound(
        @PathVariable matchId: Int,
        @PathVariable roundNumber: Int,
        user: AuthenticatedUser,
    ): ResponseEntity<*> {
        val result = matchService.endRound(user, matchId, roundNumber)
        return when (result) {
            is Either.Left ->
                when (result.value) {
                    MatchError.MatchNotFound -> Problem.response(404, Problem.matchNotFound)
                    MatchError.RoundNotFound -> Problem.response(404, Problem.roundNotFound)
                    else -> Problem.response(400, Problem.invalidRequestContent)
                }
            is Either.Right ->
                ResponseEntity
                    .status(200)
                    .body(EndRoundModel(matchId = matchId, roundNumber = roundNumber))
        }
    }

    @PostMapping(Routes.MATCH.START_ROUND)
    fun startNewRound(
        @PathVariable matchId: Int,
        user: AuthenticatedUser,
    ): ResponseEntity<*> {
        return when (val result = matchService.startNewRound(user, matchId)) {
            is Either.Left ->
                when (result.value) {
                    MatchError.MatchNotFound -> Problem.response(404, Problem.matchNotFound)
                    MatchError.NotLobbyHost -> Problem.response(403, Problem.notLobbyHost)
                    else -> Problem.response(400, Problem.invalidRequestContent)
                }
            is Either.Right -> {
                val round = result.value
                ResponseEntity.ok(
                    RoundOutputModel(
                        roundNumber = round.roundNumber,
                        pot = round.pot,
                        winnerId = round.winnerId,
                        currentPlayerId = round.currPlayerId,
                        turns = emptyList(),
                    ),
                )
            }
        }
    }
}

private fun User.toPlayerMatchOutputModel() =
    PlayerMatchOutputModel(
        id = this.id,
        name = this.name,
        balance = this.balance,
    )
