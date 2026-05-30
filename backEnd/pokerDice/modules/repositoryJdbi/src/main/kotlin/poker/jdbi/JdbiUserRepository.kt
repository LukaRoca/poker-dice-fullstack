package poker.jdbi

import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import org.jdbi.v3.core.Handle
import poker.PasswordValidationInfo
import poker.invite.Invite
import poker.token.TokenValidationInfo
import poker.user.User
import poker.utils.Token
import transaction.repository.UserRepository

class JdbiUserRepository(
    private val handle: Handle,
) : UserRepository {
    override fun createUser(
        name: String,
        passwordValidation: PasswordValidationInfo,
        balance: Int,
    ): User =
        handle
            .createUpdate(
                """
                INSERT INTO dbo.user (name, password_validation, balance)
                VALUES (:name, :password_validation, :balance)
                RETURNING *
                """,
            ).bind("name", name)
            .bind("password_validation", passwordValidation.validationInfo)
            .bind("balance", balance)
            .executeAndReturnGeneratedKeys()
            .map { rs, _ ->
                User(
                    rs.getInt("id"),
                    rs.getString("name"),
                    PasswordValidationInfo(rs.getString("password_validation")),
                    rs.getInt("balance"),
                    rs.getInt("rounds_played"),
                    rs.getInt("rounds_won")
                )
            }.one()

    override fun hasAnyUser(): Boolean =
        handle
            .createQuery("SELECT EXISTS (SELECT 1 FROM dbo.user)")
            .mapTo(Boolean::class.java)
            .one()

    override fun updateBalance(userId: Int, newBalance: Int) {
        handle.createUpdate(
            """
        UPDATE dbo.USER
        SET balance = :newBalance
        WHERE id = :userId
        """
        )
            .bind("newBalance", newBalance)
            .bind("userId", userId)
            .execute()
    }

    override fun createInvite(
        inviterId: Int,
        codeHash: String,
        createdAt: Instant,
    ): Invite =
        handle
            .createUpdate(
                """
                INSERT INTO dbo.APP_INVITE (inviterId, invite_code_hash, createdAt)
                VALUES (:inviterId, :codeHash, :createdAt)
                RETURNING *
                """,
            ).bind("inviterId", inviterId)
            .bind("codeHash", codeHash)
            .bind("createdAt", createdAt.toJavaInstant())
            .executeAndReturnGeneratedKeys()
            .map(::mapToInvite)
            .one()

    override fun getInviteByHash(codeHash: String): Invite? =
        handle
            .createQuery("SELECT * FROM dbo.APP_INVITE WHERE invite_code_hash = :codeHash")
            .bind("codeHash", codeHash)
            .map(::mapToInvite)
            .singleOrNull()

    override fun markInviteAsUsed(
        inviteId: Int,
        usedAt: Instant,
    ) {
        handle
            .createUpdate("UPDATE dbo.APP_INVITE SET usedAt = :usedAt WHERE id = :id")
            .bind("usedAt", usedAt.toJavaInstant())
            .bind("id", inviteId)
            .execute()
    }

    override fun getUserByName(name: String): User? =
        handle
            // Faltavam aqui as colunas também
            .createQuery("SELECT id, name, password_validation, balance, rounds_played, rounds_won FROM dbo.user WHERE name = :name")
            .bind("name", name)
            .map { rs, _, _ ->
                User(
                    rs.getInt("id"),
                    rs.getString("name"),
                    PasswordValidationInfo(rs.getString("password_validation")),
                    rs.getInt("balance"),
                    rs.getInt("rounds_played"),
                    rs.getInt("rounds_won")
                )
            }.singleOrNull()

    override fun getTokenByTokenValidationInfo(tokenValidationInfo: TokenValidationInfo): Pair<User, Token>? =
        handle
            .createQuery(
                """
                SELECT u.id as user_id, u.name, u.password_validation, u.balance,
                       u.rounds_played, u.rounds_won,
                       t.token_validation, t.created_at, t.last_used_at
                FROM dbo.user u
                JOIN dbo.tokens t ON u.id = t.user_id
                WHERE t.token_validation = :validation
                """,
            ).bind("validation", tokenValidationInfo.validationInfo)
            .map { rs, _, _ ->
                val user =
                    User(
                        id = rs.getInt("user_id"),
                        name = rs.getString("name"),
                        password = PasswordValidationInfo(rs.getString("password_validation")),
                        balance = rs.getInt("balance"),
                        roundsPlayed = rs.getInt("rounds_played"), // O código já tentava ler isto
                        roundsWon = rs.getInt("rounds_won")        // O código já tentava ler isto
                    )
                val token =
                    Token(
                        TokenValidationInfo(rs.getString("token_validation")),
                        user.id,
                        rs.getTimestamp("created_at").toInstant().toKotlinInstant(),
                        rs.getTimestamp("last_used_at").toInstant().toKotlinInstant(),
                    )
                user to token
            }.singleOrNull()

    override fun createToken(
        token: Token,
        maxTokens: Int,
    ): Int {
        val existingTokenIds =
            handle
                .createQuery("SELECT id FROM dbo.tokens WHERE user_id = :user_id ORDER BY last_used_at ASC")
                .bind("user_id", token.userId)
                .mapTo(Int::class.java)
                .list()

        if (existingTokenIds.size >= maxTokens) {
            val tokensToRemove = existingTokenIds.size - maxTokens + 1
            val idsToRemove = existingTokenIds.take(tokensToRemove)
            handle
                .createUpdate("DELETE FROM dbo.tokens WHERE id IN (<ids>)")
                .bindList("ids", idsToRemove)
                .execute()
        }

        return handle
            .createUpdate(
                """
                INSERT INTO dbo.tokens(user_id, token_validation, created_at, last_used_at)
                VALUES (:user_id, :token_validation, :created_at, :last_used_at)
                """,
            ).bind("user_id", token.userId)
            .bind("token_validation", token.tokenValidationInfo.validationInfo)
            .bind("created_at", token.createdAt.toJavaInstant())
            .bind("last_used_at", token.lastUsedAt.toJavaInstant())
            .executeAndReturnGeneratedKeys("id")
            .mapTo(Int::class.java)
            .one()
    }

    override fun updateTokenLastUsed(
        token: Token,
        now: Instant,
    ): Int =
        handle
            .createUpdate("UPDATE dbo.tokens SET last_used_at = :last_used_at WHERE token_validation = :validation")
            .bind("last_used_at", now.toJavaInstant())
            .bind("validation", token.tokenValidationInfo.validationInfo)
            .execute()

    override fun removeTokenByValidationInfo(tokenValidationInfo: TokenValidationInfo): Int =
        handle
            .createUpdate("DELETE FROM dbo.tokens WHERE token_validation = :validation_information")
            .bind("validation_information", tokenValidationInfo.validationInfo)
            .execute()

    override fun doesUserExistByName(name: String): Boolean =
        handle
            .createQuery("SELECT COUNT(*) FROM dbo.user WHERE name = :name")
            .bind("name", name)
            .mapTo(Int::class.java)
            .one() > 0

    override fun updateUserStats(userId: Int, wonRound: Boolean) {
        val wonIncrement = if (wonRound) 1 else 0

        handle.createUpdate(
            """
            UPDATE dbo.user 
            SET rounds_played = rounds_played + 1,
                rounds_won = rounds_won + :wonIncrement
            WHERE id = :userId
            """
        )
            .bind("userId", userId)
            .bind("wonIncrement", wonIncrement)
            .execute()
    }
    private fun mapToInvite(
        rs: java.sql.ResultSet,
        ctx: org.jdbi.v3.core.statement.StatementContext,
    ): Invite =
        Invite(
            id = rs.getInt("id"),
            inviterId = rs.getInt("inviterId"),
            codeHash = rs.getString("invite_code_hash"),
            createdAt = rs.getTimestamp("createdAt").toInstant().toKotlinInstant(),
            usedAt = rs.getTimestamp("usedAt")?.toInstant()?.toKotlinInstant(),
        )
}
