package poker.user

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import poker.PasswordValidationInfo
import poker.token.TokenEncoder
import poker.token.TokenValidationInfo
import poker.utils.Token
import java.security.SecureRandom
import java.util.Base64

@Component
class UserDomain(
    private val passwordEncoder: PasswordEncoder,
    private val tokenEncoder: TokenEncoder,
    private val config: UserDomainConfig,
) {
    val maxNumberOfTokensPerUser = config.maxTokensPerUser

    fun generateTokenValue(): String =
        ByteArray(config.tokenSizeInBytes).let { byteArray ->
            SecureRandom.getInstanceStrong().nextBytes(byteArray)
            Base64.getUrlEncoder().encodeToString(byteArray)
        }

    fun canBeToken(token: String): Boolean =
        try {
            Base64
                .getUrlDecoder()
                .decode(token)
                .size == config.tokenSizeInBytes
        } catch (_: IllegalArgumentException) {
            false
        }

    fun createTokenValidationInformation(token: String): TokenValidationInfo = tokenEncoder.createValidationInformation(token)

    fun createPasswordValidationInformation(password: String) =
        PasswordValidationInfo(
            validationInfo = passwordEncoder.encode(password),
        )

    fun validatePassword(
        password: String,
        validationInfo: String,
    ) = passwordEncoder.matches(
        password,
        validationInfo,
    )

    fun getTokenExpiration(token: Token): Instant {
        val absoluteExpiration = token.createdAt + config.tokenTtl
        val rollingExpiration = token.lastUsedAt + config.tokenRollingTtl
        return if (absoluteExpiration < rollingExpiration) {
            absoluteExpiration
        } else {
            rollingExpiration
        }
    }

    fun isTokenTimeValid(
        clock: Clock,
        token: Token,
    ): Boolean {
        val now = clock.now()
        return token.createdAt <= now &&
            (now - token.createdAt) <= config.tokenTtl &&
            (now - token.lastUsedAt) <= config.tokenRollingTtl
    }

    fun isSafePassword(password: String): Boolean {
        if (password.length < config.minPasswordLength) {
            return false
        }
        val hasDigit = password.any { it.isDigit() }
        val hasUpperCase = password.any { it.isUpperCase() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }

        return hasUpperCase && hasSpecialChar && hasDigit
    }
}
