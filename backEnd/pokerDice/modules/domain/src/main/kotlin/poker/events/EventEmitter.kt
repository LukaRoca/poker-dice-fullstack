package poker.events

interface EventEmitter {
    fun emit(event: LobbyEvent)

    fun emit(event: GlobalEvent)

    fun complete()

    fun onCompletion(callback: () -> Unit)

    fun onError(callback: (Throwable) -> Unit)
}
