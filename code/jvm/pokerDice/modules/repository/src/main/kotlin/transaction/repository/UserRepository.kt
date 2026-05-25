package transaction.repository

import kotlinx.datetime.Instant
import poker.PasswordValidationInfo
import poker.invite.Invite
import poker.token.TokenValidationInfo
import poker.user.User
import poker.utils.Token

interface UserRepository {
    fun createUser(
        name: String,
        passwordValidation: PasswordValidationInfo,
        balance: Int,
    ): User

    fun updateBalance(userId: Int, newBalance: Int)

    fun getUserByName(name: String): User?

    fun doesUserExistByName(name: String): Boolean

    fun updateUserStats(userId: Int, wonRound: Boolean)

    fun createToken(
        token: Token,
        maxTokens: Int,
    ): Int

    fun getTokenByTokenValidationInfo(tokenValidationInfo: TokenValidationInfo): Pair<User, Token>?

    fun updateTokenLastUsed(
        token: Token,
        now: Instant,
    ): Int

    fun removeTokenByValidationInfo(tokenValidationInfo: TokenValidationInfo): Int

    fun createInvite(
        inviterId: Int,
        codeHash: String,
        createdAt: Instant,
    ): Invite

    fun getInviteByHash(codeHash: String): Invite?

    fun markInviteAsUsed(
        inviteId: Int,
        usedAt: Instant,
    )

    fun hasAnyUser(): Boolean
}
