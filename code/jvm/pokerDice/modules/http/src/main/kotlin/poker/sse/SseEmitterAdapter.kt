package poker.sse

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import poker.events.EventEmitter
import poker.events.GlobalEvent
import poker.events.LobbyEvent

class SseEmitterAdapter(
    private val sseEmitter: SseEmitter,
) : EventEmitter {

    override fun emit(event: LobbyEvent) {
        val sseEventBuilder = SseEmitter.event()

        // Criamos um Map explicitamente para garantir que o campo "type" segue no JSON
        val data = when (event) {
            is LobbyEvent.PlayerJoined -> mapOf(
                "type" to event.type,
                "lobbyId" to event.lobbyId,
                "userId" to event.userId,
                "username" to event.username
            )
            is LobbyEvent.PlayerLeft -> mapOf(
                "type" to event.type,
                "lobbyId" to event.lobbyId,
                "userId" to event.userId,
                "username" to event.username
            )
            is LobbyEvent.MatchStarted -> mapOf(
                "type" to event.type,
                "lobbyId" to event.lobbyId,
                "matchId" to event.matchId
            )
            is LobbyEvent.KeepAlive -> null
        }

        if (data != null) {
            sseEventBuilder.data(data)
        } else if (event is LobbyEvent.KeepAlive) {
            sseEventBuilder.comment(event.timestamp.toString())
        }

        try {
            sseEmitter.send(sseEventBuilder)
        } catch (e: Exception) {
        }
    }

    override fun emit(event: GlobalEvent) {
        val sseEventBuilder = SseEmitter.event()

        // Igual para GlobalEvent: forçamos o "type" no Map
        val data = when (event) {
            is GlobalEvent.InviteReceived -> mapOf(
                "type" to event.type,
                "fromUsername" to event.fromUsername,
                "lobbyId" to event.lobbyId,
                "lobbyName" to event.lobbyName
            )
            is GlobalEvent.MatchStarted -> mapOf(
                "type" to event.type,
                "matchId" to event.matchId
            )
            is GlobalEvent.KeepAlive -> null
        }

        if (data != null) {
            sseEventBuilder.data(data)
        } else if (event is GlobalEvent.KeepAlive) {
            sseEventBuilder.comment(event.timestamp.toString())
        }

        try {
            sseEmitter.send(sseEventBuilder)
        } catch (e: Exception) {
        }
    }

    override fun onCompletion(callback: () -> Unit) = sseEmitter.onCompletion(callback)

    override fun onError(callback: (Throwable) -> Unit) = sseEmitter.onError(callback)

    override fun complete() = sseEmitter.complete()
}