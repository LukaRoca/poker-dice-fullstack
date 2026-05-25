package poker.controllers

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import poker.LobbyError
import poker.LobbyServices
import poker.MatchError
import poker.MatchServices
import poker.Routes
import poker.lobby.Lobby
import poker.lobby.LobbyStatus
import poker.model.*
import poker.sse.SseEmitterAdapter
import poker.user.AuthenticatedUser
import poker.user.User
import poker.utils.Either
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

@RestController
class LobbyController(
    private val lobbyService: LobbyServices,
    private val matchService: MatchServices,
) {
    @PostMapping(Routes.LOBBY.CREATE, consumes = ["application/json"], produces = ["application/json"])
    fun createLobby(
        @RequestBody input: CreateLobbyInputModel,
        user: AuthenticatedUser,
    ): ResponseEntity<*> {
        val res = lobbyService.createLobby(
            input.name, input.isPublic, user.user,
            input.rounds, input.expectedPlayers, input.ante, input.timeout,
        )
        return when (res) {
            is Either.Right -> {
                val lobby = res.value
                ResponseEntity
                    .status(201)
                    .header("Location", "/api/lobbies/${lobby.id.id}")
                    .body(lobby.toDetailOutputModel())
            }
            is Either.Left -> when (res.value) {
                LobbyError.LobbyAlreadyExists -> Problem.response(409, Problem.lobbyAlreadyExists)
                LobbyError.InsufficientFunds -> Problem.response(400, Problem.insufficientBalance)
                else -> Problem.response(400, Problem.invalidRequestContent)
            }
        }
    }

    @GetMapping(Routes.LOBBY.GET_ALL, produces = ["application/json"])
    fun listLobbies(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") pageSize: Int
    ): ResponseEntity<*> {
        val res = lobbyService.listAvailableLobbies(page, pageSize)
        return when (res) {
            is Either.Right -> {
                val lobbies = res.value
                val totalPages = ceil(lobbies.total.toDouble() / pageSize.toDouble()).toInt()
                ResponseEntity.ok()
                    .header("Cache-Control", "public, max-age=5")
                    .body(LobbyListOutputModel(
                        items = lobbies.items.map { it.toSummaryOutputModel() },
                        total = lobbies.total,
                        page = page,
                        pageSize = pageSize,
                        totalPages = totalPages
                    ))
            }
            is Either.Left -> when (res.value) {
                LobbyError.NoLobbiesExist -> {
                    ResponseEntity.ok()
                        .header("Cache-Control", "public, max-age=5")
                        .body(LobbyListOutputModel(
                            items = emptyList(),
                            total = 0,
                            page = page,
                            pageSize = pageSize,
                            totalPages = 0
                        ))
                }
                else -> Problem.response(400, Problem.invalidRequestContent)
            }
        }
    }

    @GetMapping(Routes.LOBBY.GET_BY_ID, produces = ["application/json"])
    fun getLobby(
        @PathVariable lobbyId: Int,
    ): ResponseEntity<*> {
        val matchId = lobbyService.getMatchIdForLobby(lobbyId)
        if (matchId != null) {
            return ResponseEntity.status(200)
                .header("Cache-Control", "no-store")
                .body(LobbyRedirectOutputModel(
                    lobbyId = lobbyId,
                    matchId = matchId,
                    redirect = true,
                    message = "Partida em andamento"
                ))
        }

        val res = lobbyService.getLobbyById(lobbyId)
        return when (res) {
            is Either.Right -> {
                val lobby = res.value
                ResponseEntity.ok()
                    .header("Cache-Control", "no-store")
                    .body(lobby.toDetailOutputModel(matchId))
            }
            is Either.Left -> when (res.value) {
                LobbyError.LobbyNotFound -> {
                    Problem.response(404, Problem.nolobbysFound)
                }
                else -> Problem.response(400, Problem.invalidRequestContent)
            }
        }
    }

    @PostMapping(Routes.LOBBY.JOIN, produces = ["application/json"])
    fun joinLobby(
        @PathVariable lobbyId: Int,
        user: AuthenticatedUser,
    ): ResponseEntity<*> {
        val res = lobbyService.joinLobby(lobbyId, user.user)
        return when (res) {
            is Either.Right -> ResponseEntity.ok().body(mapOf("message" to "Entrou no lobby com sucesso."))
            is Either.Left -> when (res.value) {
                LobbyError.LobbyNotFound -> Problem.response(404, Problem.nolobbysFound)
                LobbyError.AlreadyInLobby -> Problem.response(409, Problem.userAlreadyInLobby)
                LobbyError.LobbyFull -> Problem.response(409, Problem.lobbyIsFull)
                LobbyError.InsufficientFunds -> Problem.response(400, Problem.insufficientBalance)
                else -> Problem.response(400, Problem.invalidRequestContent)
            }
        }
    }

    @PostMapping(Routes.LOBBY.LEAVE, produces = ["application/json"])
    fun leaveLobby(
        @PathVariable lobbyId: Int,
        user: AuthenticatedUser,
    ): ResponseEntity<*> {
        val res = lobbyService.leaveLobby(lobbyId, user.user.id)
        return when (res) {
            is Either.Right -> ResponseEntity.ok().body(mapOf("message" to "Saiu do lobby com sucesso."))
            is Either.Left -> when (res.value) {
                LobbyError.LobbyNotFound -> Problem.response(404, Problem.nolobbysFound)
                LobbyError.NotInLobby -> Problem.response(400, Problem.userNotInLobby)
                else -> Problem.response(400, Problem.invalidRequestContent)
            }
        }
    }

    @DeleteMapping(Routes.LOBBY.DELETE, produces = ["application/json"])
    fun deleteLobby(@PathVariable lobbyId: Int, user: AuthenticatedUser): ResponseEntity<*> {
        val res = lobbyService.deleteLobby(lobbyId, user.user.id)
        return when (res) {
            is Either.Right -> ResponseEntity.ok().body(mapOf("message" to "Lobby apagado com sucesso."))
            is Either.Left -> when (res.value) {
                LobbyError.LobbyNotFound -> Problem.response(404, Problem.nolobbysFound)
                LobbyError.NotLobbyHost -> Problem.response(403, Problem.notLobbyHost)
                else -> Problem.response(400, Problem.invalidRequestContent)
            }
        }
    }

    @PostMapping(Routes.LOBBY.START, produces = ["application/json"])
    fun startMatch(
        @PathVariable lobbyId: Int,
        user: AuthenticatedUser,
    ): ResponseEntity<*> =
        when (val createResult = matchService.startMatch(user, lobbyId)) {
            is Either.Left -> when (createResult.value) {
                is MatchError.NotLobbyHost -> Problem.response(403, Problem.notLobbyHost)
                is MatchError.NotEnoughPlayers -> Problem.response(409, Problem.notEnoughPlayers)
                is MatchError.MatchAlreadyStarted -> Problem.response(409, Problem.matchAlreadyStarted)
                is MatchError.MatchNotFound -> Problem.response(404, Problem.matchNotFound)
                else -> Problem.response(400, Problem.invalidRequestContent)
            }
            is Either.Right -> {
                val match = createResult.value
                lobbyService.notifyMatchStarted(lobbyId, match.matchId)
                ResponseEntity.status(201).body(MatchByIdModel(match.matchId))
            }
        }

    @GetMapping(Routes.LOBBY.LISTEN)
    fun listenToLobby(
        @PathVariable lobbyId: Int,
        user: AuthenticatedUser,
    ): ResponseEntity<SseEmitter> {
        val sseEmitter = SseEmitter(TimeUnit.HOURS.toMillis(1))
        val eventEmitterAdapter = SseEmitterAdapter(sseEmitter)
        lobbyService.addLobbyListener(lobbyId, eventEmitterAdapter)
        return ResponseEntity.status(200)
            .header("Content-Type", "text/event-stream; charset=utf-8")
            .header("Connection", "keep-alive")
            .header("X-Accel-Buffering", "no")
            .body(sseEmitter)
    }
}

private fun Lobby.toDetailOutputModel(matchId: Int? = null) =
    LobbyDetailOutputModel(
        id = this.id.id,
        name = this.name.name,
        description = this.description,
        host = this.host.toPlayerOutputModel(),
        rounds = this.rounds,
        ante = this.ante,
        expectedPlayers = this.expectedPlayers.count,
        state = this.status.name,
        players = this.players.map { it.toPlayerOutputModel() },
        matchId = matchId
    )

private fun Lobby.toSummaryOutputModel() =
    LobbySummaryOutputModel(
        id = this.id.id,
        name = this.name.name,
        hostUsername = this.host.name,
        currentPlayerCount = this.players.size,
        expectedPlayers = this.expectedPlayers.count,
        state = this.status.name,
    )

private fun User.toPlayerOutputModel() =
    PlayerOutputModel(id = this.id, name = this.name)