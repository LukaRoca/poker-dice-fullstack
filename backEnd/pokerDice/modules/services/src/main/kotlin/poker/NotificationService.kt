package poker

import jakarta.inject.Named
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import poker.events.EventEmitter
import poker.events.GlobalEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

interface NeedsShutdown {
    fun shutdown()
}

@Named
class NotificationService : NeedsShutdown {
    companion object {
        private val logger = LoggerFactory.getLogger(NotificationService::class.java)
    }

    private val listeners = ConcurrentHashMap<Int, MutableList<EventEmitter>>()
    private val lock = ReentrantLock()

    private val scheduler: ScheduledExecutorService =
        Executors.newScheduledThreadPool(1).also {
            it.scheduleAtFixedRate({ keepAlive() }, 15, 15, TimeUnit.SECONDS)
        }

    override fun shutdown() {
        logger.info("A desligar o NotificationService...")
        scheduler.shutdown()
    }

    fun addEventEmitter(
        listener: EventEmitter,
        userId: Int,
    ) = lock.withLock {
        logger.info("A adicionar listener global para o utilizador {}", userId)
        val userListeners =
            listeners.computeIfAbsent(userId) {
                mutableListOf<EventEmitter>()
            }

        synchronized(userListeners) {
            userListeners.add(listener)
        }

        val removalCallback: () -> Unit = {
            lock.withLock {
                logger.info("A remover listener global do utilizador {}", userId)
                synchronized(userListeners) {
                    userListeners.remove(listener)
                    if (userListeners.isEmpty()) {
                        listeners.remove(userId)
                    }
                }
            }
        }
        listener.onCompletion(removalCallback)
        listener.onError { removalCallback() }
    }

    fun sendEventToUser(
        userId: Int,
        event: GlobalEvent,
    ) {
        val userListeners = listeners[userId] ?: return

        logger.info("A enviar evento global {} para o utilizador {}", event.type, userId)
        synchronized(userListeners) {
            userListeners.toList().forEach { emitter ->
                try {
                    emitter.emit(event)
                } catch (e: Exception) {
                    logger.warn("Falha ao enviar evento para o utilizador {}: {}", userId, e.message)
                }
            }
        }
    }

    private fun keepAlive() =
        lock.withLock {
            val keepAliveEvent = GlobalEvent.KeepAlive(Clock.System.now())
            listeners.values.forEach { list ->
                synchronized(list) {
                    list.toList().forEach { emitter ->
                        try {
                            emitter.emit(keepAliveEvent)
                        } catch (ex: Exception) {
                        }
                    }
                }
            }
        }
}
