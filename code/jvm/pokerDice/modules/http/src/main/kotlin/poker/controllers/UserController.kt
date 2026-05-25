package poker.controllers

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import poker.NotificationService
import poker.Routes
import poker.TokenCreationError
import poker.TokenExternalInfo
import poker.UserCreationResult
import poker.UserError
import poker.UsersServices
import poker.invite.InviteDomainConfig
import poker.model.DepositInputModel
import poker.model.InviteOutputModel
import poker.model.LogoutDetailOutputModel
import poker.model.Problem
import poker.model.UserLoginInfoOutputModel
import poker.model.UserLoginInputModel
import poker.model.UserLoginOutputModel
import poker.model.UserProfileOutputModel
import poker.model.UserRegisterInputModel
import poker.model.UserRegisterOutputModel
import poker.sse.SseEmitterAdapter
import poker.user.AuthenticatedUser
import poker.utils.Either
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@RestController
class UserController(
    private val userService: UsersServices,
    private val inviteConfig: InviteDomainConfig,
    private val notificationService: NotificationService,
) {
    @PostMapping(Routes.User.REGISTER)
    fun register(
        @RequestBody input: UserRegisterInputModel,
    ): ResponseEntity<*> {
        val res: UserCreationResult = userService.register(input.name, input.password, input.inviteCode)
        return when (res) {
            is Either.Right -> {
                val user = res.value
                ResponseEntity
                    .status(201)
                    .header("Location", "${Routes.User.GET_USER_BY_NAME}/${user.name}")
                    .header("Cache-Control", "no-store")
                    .body(UserRegisterOutputModel(id = user.id, name = user.name))
            }
            is Either.Left -> {
                val error = res.value
                when (error) {
                    UserError.UserAlreadyExists -> Problem.response(409, Problem.userAlreadyExists)
                    UserError.InsecurePassword -> Problem.response(400, Problem.insecurePassword)
                    UserError.InvitationDontExist -> Problem.response(404, Problem.invitationDontExist)
                    UserError.InvitationExpired -> Problem.response(410, Problem.invitationExpired)
                    UserError.InvitationUsed -> Problem.response(409, Problem.invitationUsed)
                    UserError.InvitationRequired -> Problem.response(403, Problem.invitationRequired)
                    UserError.UserNotFound, UserError.InvalidDepositAmount -> Problem.response(400, Problem.internalServerError)
                }
            }
        }
    }

    @PostMapping(Routes.User.LOGIN)
    fun login(
        @RequestBody input: UserLoginInputModel,
    ): ResponseEntity<*> {
        val res = userService.createToken(input.name, input.password)
        return when (res) {
            is Either.Right -> {
                val (tokenInfo, user) = res.value
                val response =
                    UserLoginOutputModel(
                        token = tokenInfo.tokenValue,
                        user =
                            UserLoginInfoOutputModel(
                                id = user.id,
                                name = user.name,
                                balance = user.balance,
                            ),
                    )
                ResponseEntity.status(200)
                    .header("Cache-Control", "no-store")
                    .body(response)
            }
            is Either.Left ->
                when (res.value) {
                    TokenCreationError.UserOrPasswordAreInvalid ->
                        Problem.response(401, Problem.userOrPasswordAreInvalid)
                }
        }
    }

    @PostMapping(Routes.User.DEPOSIT)
    fun deposit(
        user: AuthenticatedUser,
        @RequestBody input: DepositInputModel
    ): ResponseEntity<*> {
        return when (val result = userService.deposit(user.user.name, input.amount)) {
            is Either.Right -> ResponseEntity.ok(mapOf("newBalance" to result.value))
            is Either.Left -> when (result.value) {
                UserError.InvalidDepositAmount -> Problem.response(400, Problem.invalidDepositAmount)
                UserError.UserNotFound -> Problem.response(404, Problem.userNotFound)
                else -> Problem.response(500, Problem.internalServerError)
            }
        }
    }

    @GetMapping(Routes.User.GET_USER_BY_NAME)
    fun getUserByName(@PathVariable name: String): ResponseEntity<UserProfileOutputModel> {
        val user = userService.getUserByName(name) ?: return ResponseEntity.notFound().build()

        val calculatedWinRate = if (user.roundsPlayed > 0) {
            (user.roundsWon.toDouble() / user.roundsPlayed.toDouble()) * 100
        } else {
            0.0
        }

        return ResponseEntity.ok()
            .header("Cache-Control", "public, max-age=300")
            .body(UserProfileOutputModel(
                id = user.id,
                name = user.name,
                balance = user.balance,
                roundsPlayed = user.roundsPlayed,
                roundsWon = user.roundsWon,
                winRate = calculatedWinRate
            ))
    }

    @GetMapping(Routes.User.LISTEN)
    fun listen(authenticatedUser: AuthenticatedUser): ResponseEntity<SseEmitter> {
        val sseEmitter = SseEmitter(TimeUnit.HOURS.toMillis(1))
        notificationService.addEventEmitter(
            SseEmitterAdapter(sseEmitter),
            authenticatedUser.user.id,
        )
        return ResponseEntity
            .status(200)
            .header("Content-Type", "text/event-stream; charset=utf-8")
            .header("Connection", "keep-alive")
            .header("X-Accel-Buffering", "no")
            .header("Cache-Control", "no-store")
            .body(sseEmitter)
    }

    @PostMapping(Routes.User.INVITE)
    fun createInvite(userAuthenticatedUser: AuthenticatedUser): ResponseEntity<*> {
        val (plainTextCode, createdInvite) = userService.createInvite(userAuthenticatedUser.user.id)

        val expiresAtInstant = createdInvite.createdAt + inviteConfig.validityDuration
        val localExpiresAt = expiresAtInstant.toLocalDateTime(TimeZone.currentSystemDefault())
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val formattedDate = localExpiresAt.toJavaLocalDateTime().format(formatter)

        val responseBody = InviteOutputModel(inviteCode = plainTextCode, expiresAt = formattedDate)

        return ResponseEntity.status(201)
            .header("Cache-Control", "no-store")
            .body(responseBody)
    }

    @PostMapping(Routes.User.TOKEN_REFRESH)
    fun refreshToken(
        @RequestHeader("Authorization") authorizationHeader: String,
    ): ResponseEntity<TokenExternalInfo> {
        val oldToken = authorizationHeader.removePrefix("Bearer ").trim()
        val newTokenInfo = userService.refreshToken(oldToken)
            ?: return ResponseEntity.status(401).build()
        return ResponseEntity.ok()
            .header("Cache-Control", "no-store")
            .body(newTokenInfo)
    }

    @PostMapping(Routes.User.LOGOUT)
    fun logout(
        @RequestHeader("Authorization") authorizationHeader: String,
    ): ResponseEntity<LogoutDetailOutputModel> {
        val token = authorizationHeader.removePrefix("Bearer ").trim()
        userService.revokeToken(token)
        return ResponseEntity.ok()
            .header("Cache-Control", "no-store")
            .body(LogoutDetailOutputModel(token = token))
    }

    @GetMapping(Routes.User.PROFILE)
    fun getMyProfile(userAuthenticatedUser: AuthenticatedUser): ResponseEntity<UserProfileOutputModel> {
        val user = userAuthenticatedUser.user

        // Cálculo da Win Rate (evitar divisão por zero)
        val calculatedWinRate = if (user.roundsPlayed > 0) {
            (user.roundsWon.toDouble() / user.roundsPlayed.toDouble()) * 100
        } else {
            0.0
        }

        val userProfile = UserProfileOutputModel(
            id = user.id,
            name = user.name,
            balance = user.balance,
            roundsPlayed = user.roundsPlayed,
            roundsWon = user.roundsWon,
            winRate = calculatedWinRate
        )

        return ResponseEntity.ok()
            .header("Cache-Control", "public, max-age=300")
            .body(userProfile)
    }
}

