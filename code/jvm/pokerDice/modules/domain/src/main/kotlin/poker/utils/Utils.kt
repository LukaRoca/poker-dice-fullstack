package poker.utils

import kotlinx.datetime.Instant
import poker.token.TokenValidationInfo

data class Id(
    val id: Int,
) {
    init {
        if (id < 0) throw IllegalArgumentException("Id must be non-negative")
    }
}

data class Name(
    val name: String,
)

data class ExpectedPlayers(
    val count: Int,
) {
    init {
        if (count < 2) throw IllegalArgumentException("Players count must be at least 2")
    }
}

class Token(
    val tokenValidationInfo: TokenValidationInfo,
    val userId: Int,
    val createdAt: Instant,
    val lastUsedAt: Instant,
)
