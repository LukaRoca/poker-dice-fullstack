package poker.user

import poker.PasswordValidationInfo

data class User(
    val id: Int,
    val name: String,
    val password: PasswordValidationInfo,
    var balance: Int,
    val roundsPlayed: Int,
    val roundsWon: Int
) {
    val winRate: Double
        get() = if (roundsPlayed == 0) 0.0 else (roundsWon.toDouble() / roundsPlayed) * 100
}
