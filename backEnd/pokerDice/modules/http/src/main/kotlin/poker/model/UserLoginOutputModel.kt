package poker.model

data class UserLoginOutputModel(
    val token: String,
    val user: UserLoginInfoOutputModel,
)
