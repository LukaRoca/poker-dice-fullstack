package poker

import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory

@Component
class ShutdownManager(
    private val needShutdown: List<NeedsShutdown>,
) {
    private val logger = LoggerFactory.getLogger(ShutdownManager::class.java)

    @PreDestroy
    private fun preDestroy() {
        logger.info("A encerrar serviços...")
        needShutdown.forEach {
            it.shutdown()
        }
    }
}