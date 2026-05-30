package poker.pipeline

import jakarta.servlet.http.Cookie
import org.slf4j.LoggerFactory // <--- Importar Logger
import org.springframework.stereotype.Component
import poker.UsersServices
import poker.user.AuthenticatedUser

@Component
class RequestTokenProcessor(
    val usersService: UsersServices,
) {
    // Adiciona o Logger
    companion object {
        const val SCHEME = "bearer"
        private val logger = LoggerFactory.getLogger(RequestTokenProcessor::class.java)
    }

    fun processAuthorizationHeaderValue(authorizationValue: String?): AuthenticatedUser? {
        if (authorizationValue == null) {
            return null
        }
        val parts = authorizationValue.trim().split(" ")
        if (parts.size != 2) {
            return null
        }
        if (parts[0].lowercase() != SCHEME) {
            return null
        }
        return usersService.getUserByToken(parts[1])?.let {
            AuthenticatedUser(it, parts[1])
        }
    }

    fun processCookieToken(cookies: Array<Cookie>?): AuthenticatedUser? {
        // Log para ver se os cookies estão a chegar
        if (cookies == null) {
            logger.warn("🔍 [Auth] Array de cookies recebido é NULO.")
            return null
        }

        val tokenCookie = cookies.find { it.name == "token" }

        if (tokenCookie == null) {
            logger.warn("🔍 [Auth] Cookie 'token' NÃO encontrado. Cookies presentes: ${cookies.map { it.name }}")
            return null
        }

        tokenCookie.value?.let { token ->
            val user = usersService.getUserByToken(token)
            if (user != null) {
                logger.info("✅ [Auth] Autenticação via Cookie bem-sucedida para: ${user.name}")
                return AuthenticatedUser(user, token)
            } else {
                logger.warn("❌ [Auth] Token do cookie inválido ou expirado.")
            }
        }
        return null
    }
}