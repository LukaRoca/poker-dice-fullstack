package poker

object Routes {
    private const val API_BASE = "/api"

    object User {
        private const val USER_BASE = "$API_BASE/user"
        const val PROFILE = "$USER_BASE/me"
        const val REGISTER = "$API_BASE/register"
        const val LOGIN = "$API_BASE/login"
        const val LOGOUT = "$USER_BASE/logout"
        const val GET_USER_BY_NAME = "$API_BASE/users/{name}"
        const val INVITE = "$USER_BASE/invite"
        const val TOKEN_REFRESH = "$API_BASE/token/refresh"
        const val LISTEN = "$API_BASE/users/listen"
        const val DEPOSIT = "$USER_BASE/deposit"
    }

    object LOBBY {
        private const val LOBBY_BASE = "$API_BASE/lobbies"
        const val GET_ALL = LOBBY_BASE
        const val CREATE = LOBBY_BASE
        const val GET_BY_ID = "$LOBBY_BASE/{lobbyId}"
        const val JOIN = "$LOBBY_BASE/{lobbyId}/join"
        const val LEAVE = "$LOBBY_BASE/{lobbyId}/leave"
        const val START = "$LOBBY_BASE/{lobbyId}/start"
        const val DELETE = "$LOBBY_BASE/{lobbyId}/delete"
        const val LISTEN = "$LOBBY_BASE/{lobbyId}/listen"
    }

    object MATCH {
        private const val MATCH_BASE = "$API_BASE/matches/{matchId}"
        const val START_ROUND = "$MATCH_BASE/rounds"
        const val PLAY = "$MATCH_BASE/rounds/{roundNumber}/play"
        const val END_TURN = "$MATCH_BASE/rounds/{roundNumber}/end-turn"
        const val END = "$MATCH_BASE/rounds/{roundNumber}/end"
        const val RESULT = "$MATCH_BASE/state"
    }
}
