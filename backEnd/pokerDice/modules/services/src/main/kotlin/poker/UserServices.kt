package poker

import jakarta.inject.Named
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import poker.invite.Invite
import poker.invite.InviteDomainConfig
import poker.invite.InviteHasher
import poker.user.User
import poker.user.UserDomain
import poker.utils.Either
import poker.utils.Token
import poker.utils.failure
import poker.utils.success
import transaction.Transaction
import transaction.TransactionManager
import java.security.SecureRandom
import java.util.Base64

data class TokenExternalInfo(
    val tokenValue: String,
    val tokenExpiration: Instant,
)

sealed class UserError {
    data object UserAlreadyExists : UserError()

    data object InsecurePassword : UserError()

    data object InvitationDontExist : UserError()

    data object InvitationExpired : UserError()

    data object InvitationUsed : UserError()

    data object InvitationRequired : UserError()

    data object UserNotFound : UserError()

    data object InvalidDepositAmount : UserError()
}

typealias UserCreationResult = Either<UserError, User>

sealed class TokenCreationError {
    data object UserOrPasswordAreInvalid : TokenCreationError()
}

typealias TokenCreationResult = Either<TokenCreationError, Pair<TokenExternalInfo, User>>

@Named
class UsersServices(
    private val transactionManager: TransactionManager,
    private val usersDomain: UserDomain,
    private val clock: Clock,
    private val inviteHasher: InviteHasher,
    private val inviteConfig: InviteDomainConfig,
) {
    fun register(
        name: String,
        password: String,
        inviteCode: String?,
    ): UserCreationResult {
        if (!usersDomain.isSafePassword(password)) {
            return failure(UserError.InsecurePassword)
        }

        when {
            inviteCode == null -> {
                return transactionManager.run {
                    if (it.usersRepository.hasAnyUser()) {
                        failure(UserError.InvitationRequired)
                    } else {
                        createUserInternal(it, name, password)
                    }
                }
            }
            inviteCode.isBlank() -> {
                return failure(UserError.InvitationDontExist)
            }
            else -> {
                val hashedCode = inviteHasher.hash(inviteCode)
                return transactionManager.run {
                    val usersRepository = it.usersRepository
                    val invite =
                        usersRepository.getInviteByHash(hashedCode)
                            ?: return@run failure(UserError.InvitationDontExist)

                    if (invite.isExpired(clock, inviteConfig.validityDuration)) {
                        return@run failure(UserError.InvitationExpired)
                    }
                    if (invite.isUsed) {
                        return@run failure(UserError.InvitationUsed)
                    }

                    usersRepository.markInviteAsUsed(invite.id, clock.now())
                    createUserInternal(it, name, password)
                }
            }
        }
    }

    fun createInvite(userid: Int): Pair<String, Invite> {
        val plainTextCode = generatePlainTextInviteCode()
        val hashedCode = inviteHasher.hash(plainTextCode)

        val createdInvite =
            transactionManager.run {
                it.usersRepository.createInvite(
                    inviterId = userid,
                    codeHash = hashedCode,
                    createdAt = clock.now(),
                )
            }
        return Pair(plainTextCode, createdInvite)
    }

    fun deposit(userName: String, amount: Int): Either<UserError, Int> =
        transactionManager.run {
            val userRepo = it.usersRepository

            if (amount <= 0) {
                return@run failure(UserError.InvalidDepositAmount)
            }

            val user = userRepo.getUserByName(userName)
                ?: return@run failure(UserError.UserNotFound)

            val newBalance = user.balance + amount
            if (newBalance < user.balance) {
                return@run failure(UserError.InvalidDepositAmount)
            }

            userRepo.updateBalance(user.id, newBalance)

            success(newBalance)
        }

    private fun generatePlainTextInviteCode(): String =
        ByteArray(16).let {
            SecureRandom.getInstanceStrong().nextBytes(it)
            Base64.getUrlEncoder().withoutPadding().encodeToString(it)
        }

    private fun createUserInternal(
        tx: Transaction,
        name: String,
        password: String,
    ): UserCreationResult {
        if (tx.usersRepository.getUserByName(name) != null) {
            return failure(UserError.UserAlreadyExists)
        }
        val passwordValidationInfo = usersDomain.createPasswordValidationInformation(password)
        val user = tx.usersRepository.createUser(name, passwordValidationInfo, balance = 0)
        return success(user)
    }

    fun createToken(
        name: String,
        password: String,
    ): TokenCreationResult {
        if (name.isBlank() || password.isBlank()) {
            return failure(TokenCreationError.UserOrPasswordAreInvalid)
        }

        return transactionManager.run {
            val usersRepository = it.usersRepository
            val user: User =
                usersRepository.getUserByName(name)
                    ?: return@run failure(TokenCreationError.UserOrPasswordAreInvalid)
            val storedHash = user.password.validationInfo
            if (!usersDomain.validatePassword(password, storedHash)) {
                return@run failure(TokenCreationError.UserOrPasswordAreInvalid)
            }
            val tokenValue = usersDomain.generateTokenValue()
            val now = clock.now()
            val newToken =
                Token(
                    usersDomain.createTokenValidationInformation(tokenValue),
                    user.id,
                    createdAt = now,
                    lastUsedAt = now,
                )
            usersRepository.createToken(newToken, usersDomain.maxNumberOfTokensPerUser)

            val tokenInfo =
                TokenExternalInfo(
                    tokenValue,
                    usersDomain.getTokenExpiration(newToken),
                )
            success(Pair(tokenInfo, user))
        }
    }

    fun getUserByToken(token: String): User? {
        if (!usersDomain.canBeToken(token)) {
            return null
        }
        return transactionManager.run {
            val usersRepository = it.usersRepository
            val tokenValidationInfo = usersDomain.createTokenValidationInformation(token)
            val userAndToken = usersRepository.getTokenByTokenValidationInfo(tokenValidationInfo)
            if (userAndToken != null && usersDomain.isTokenTimeValid(clock, userAndToken.second)) {
                usersRepository.updateTokenLastUsed(userAndToken.second, clock.now())
                userAndToken.first
            } else {
                null
            }
        }
    }

    fun revokeToken(token: String): Boolean {
        val tokenValidationInfo = usersDomain.createTokenValidationInformation(token)
        return transactionManager.run {
            it.usersRepository.removeTokenByValidationInfo(tokenValidationInfo)
            true
        }
    }

    fun getUserByName(name: String): User? {
        if (name.isBlank()) return null
        return transactionManager.run {
            it.usersRepository.getUserByName(name)
        }
    }

    fun refreshToken(oldToken: String): TokenExternalInfo? {
        val user = getUserByToken(oldToken) ?: return null
        val tokenValue = usersDomain.generateTokenValue()
        val now = clock.now()
        val newToken =
            Token(
                usersDomain.createTokenValidationInformation(tokenValue),
                user.id,
                createdAt = now,
                lastUsedAt = now,
            )
        transactionManager.run {
            it.usersRepository.createToken(newToken, usersDomain.maxNumberOfTokensPerUser)
        }
        return TokenExternalInfo(tokenValue, usersDomain.getTokenExpiration(newToken))
    }
}