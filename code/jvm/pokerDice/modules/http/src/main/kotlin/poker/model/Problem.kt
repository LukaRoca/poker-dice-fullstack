package poker.model

import org.springframework.http.ResponseEntity
import org.springframework.http.ProblemDetail
import java.net.URI

class Problem(
    val type: URI,
    val title: String,
    val detail: String? = null
) {
    companion object {
        const val MEDIA_TYPE = "application/problem+json"
        private const val BASE_URL = "https://github.com/isel-leic-daw/2025-daw-leic53d-leic53d-02/tree/main/docs/problems/"

        fun response(
            status: Int,
            problem: Problem,
        ) = ResponseEntity
            .status(status)
            .header("Content-Type", MEDIA_TYPE)
            .body<Any>(problem.toProblemDetail(status))

        val userAlreadyInLobby = Problem(URI.create("${BASE_URL}user-already-in-lobby"), "User already in Lobby", "This user has already join the lobby")
        val notLobbyHost = Problem(URI.create("${BASE_URL}not-lobby-host"), "Not lobby host", "Only the lobby host can perform this action")
        val notEnoughPlayers = Problem(URI.create("${BASE_URL}not-enough-players"), "Not enough players", "At least 2 players are required to start the match")
        val matchAlreadyStarted = Problem(URI.create("${BASE_URL}match-already-started"), "Match already started", "The match has already begun")
        val userAlreadyExists = Problem(URI.create("${BASE_URL}user-already-exists"), "User already exists", "A user with this username already exists")
        val invitationDontExist = Problem(URI.create("${BASE_URL}invite-dont-exist"), "Invitation not found", "The provided invitation code is invalid")
        val invitationRequired = Problem(URI.create("${BASE_URL}invitation-required"), "Invitation required", "An invitation code is required for registration")
        val invitationExpired = Problem(URI.create("${BASE_URL}invite-expired"), "Invitation expired", "The invitation code has expired")
        val invitationUsed = Problem(URI.create("${BASE_URL}invite-used"), "Invitation already used", "The invitation code has already been used")
        val insecurePassword = Problem(URI.create("${BASE_URL}insecure-password"), "Insecure password", "Password does not meet security requirements")
        val userOrPasswordAreInvalid = Problem(URI.create("${BASE_URL}user-or-password-invalid"), "Invalid credentials", "Username or password are incorrect")
        val invalidRequestContent = Problem(URI.create("${BASE_URL}invalid-content"), "Invalid request content", "The request content is malformed or invalid")
        val lobbyAlreadyExists = Problem(URI.create("${BASE_URL}lobby-exist"), "A lobby with that name already exists", "A lobby with this name already exists")
        val nolobbysFound = Problem(URI.create("${BASE_URL}lobby-not-found"), "No lobbies found", "There are no available lobbies")
        val userNotInLobby = Problem(URI.create("${BASE_URL}user-not-in-lobby"), "User not in lobby", "The user is not a member of this lobby")
        val lobbyIsFull = Problem(URI.create("${BASE_URL}lobby-is-full"), "Lobby is full", "The lobby has reached its maximum player capacity")
        val matchNotFound = Problem(URI.create("${BASE_URL}match-not-found"), "Match not found", "The specified match was not found")
        val roundNotFound = Problem(URI.create("${BASE_URL}round-not-found"), "Round not found", "The specified round was not found")
        val notYourTurn = Problem(URI.create("${BASE_URL}not-your-turn"), "Not your turn", "It is not your turn to play")
        val userNotFound = Problem(URI.create("${BASE_URL}user-not-found"), "User not found", "The specified user was not found")
        val invalidDepositAmount = Problem(URI.create("${BASE_URL}invalid-deposit-amount"), "Invalid deposit amount", "The deposit amount must be a positive value")
        val insufficientBalance = Problem(URI.create("${BASE_URL}insufficient-balance"), "Insufficient balance", "The user does not have enough balance to start this lobby")
        val internalServerError = Problem(URI.create("${BASE_URL}internal-server-error"), "Internal server error", "An unexpected error occurred on the server")

    }
    fun toProblemDetail(status: Int): ProblemDetail {
        val problemDetail = ProblemDetail.forStatus(status)
        problemDetail.type = this.type
        problemDetail.title = this.title
        problemDetail.detail = this.detail
        return problemDetail
    }
}