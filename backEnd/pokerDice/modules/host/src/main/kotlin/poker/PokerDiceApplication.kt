package poker

import kotlinx.datetime.Clock
import org.jdbi.v3.core.Jdbi
import org.postgresql.ds.PGSimpleDataSource
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import poker.invite.InviteDomainConfig
import poker.invite.InviteHasher
import poker.invite.Sha256InviteHasher
import poker.jdbi.configureWithAppRequirements
import poker.pipeline.AuthenticatedUserArgumentResolver
import poker.pipeline.AuthenticationInterceptor
import poker.user.Sha256TokenEncoder
import poker.user.UserDomainConfig
import kotlin.time.Duration.Companion.hours

@SpringBootApplication
class PokerDiceApplication {


    @Bean
    fun jdbi() =
        Jdbi
            .create(
                PGSimpleDataSource().apply {
                    setURL(Environment.getDbUrl())
                },
            ).configureWithAppRequirements()

    @Bean
    fun passwordEncoder() : PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun tokenEncoder() = Sha256TokenEncoder()

    @Bean
    fun inviteHasher(): InviteHasher = Sha256InviteHasher()

    @Bean
    fun clock(): Clock = Clock.System

    @Bean
    fun usersDomainConfig() =
        UserDomainConfig(
            tokenSizeInBytes = 256 / 8,
            tokenTtl = 24.hours,
            tokenRollingTtl = 1.hours,
            maxTokensPerUser = 3,
            minPasswordLength = 3,
        )

    @Bean
    fun inviteDomainConfig() =
        InviteDomainConfig(
            validityDuration = 24.hours,
        )
}

@Configuration
class PipelineConfigurer(
    val authenticationInterceptor: AuthenticationInterceptor,
    val authenticatedUserArgumentResolver: AuthenticatedUserArgumentResolver,
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(authenticationInterceptor)
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(authenticatedUserArgumentResolver)
    }
}

private val logger = LoggerFactory.getLogger("main")

fun main(args: Array<String>) {
    logger.info("Starting app")
    runApplication<PokerDiceApplication>(*args)
}
