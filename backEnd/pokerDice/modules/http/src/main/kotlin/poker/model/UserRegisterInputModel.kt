package poker.model

data class UserRegisterInputModel(
    val name: String,
    val password: String,
    val inviteCode: String?,
)
