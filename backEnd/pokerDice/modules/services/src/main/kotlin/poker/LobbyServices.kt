package poker

import jakarta.inject.Named
import poker.events.EventEmitter
import poker.events.LobbyEvent
import poker.lobby.Lobby
import poker.lobby.LobbyDomain
import poker.user.User
import poker.utils.Either
import poker.utils.failure
import poker.utils.success
import transaction.TransactionManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.min

sealed class LobbyError {
    data object LobbyNotFound : LobbyError()
    data object LobbyFull : LobbyError()
    data object LobbyAlreadyExists : LobbyError()
    data object NoLobbiesExist : LobbyError()
    data object AlreadyInLobby : LobbyError()
    data object NotInLobby : LobbyError()
    data object NotLobbyHost : LobbyError()
    data object InsufficientFunds : LobbyError()
}

data class PaginatedLobbies(
    val items: List<Lobby>,
    val total: Int
)

@Named
class LobbyServices(
    private val transactionManager: TransactionManager,
    private val lobbyDomain: LobbyDomain,
    private val matchServices: MatchServices,
) {
    private val lock = ReentrantLock()
    private val lobbyListeners = ConcurrentHashMap<Int, MutableList<EventEmitter>>()

    // Scheduler para o atraso de 3 segundos
    private val scheduler = Executors.newScheduledThreadPool(1)

    fun addLobbyListener(
        lobbyId: Int,
        emitter: EventEmitter,
    ) {
        val listeners = lobbyListeners.computeIfAbsent(lobbyId) { mutableListOf() }
        synchronized(listeners) {
            listeners.add(emitter)
        }
        val removalCallback: () -> Unit = {
            synchronized(listeners) {
                listeners.remove(emitter)
            }
        }
        emitter.onCompletion(removalCallback)
        emitter.onError { removalCallback() }
    }

    private fun notifyLobby(
        lobbyId: Int,
        event: LobbyEvent,
    ) {
        val listeners = lobbyListeners[lobbyId] ?: return
        synchronized(listeners) {
            listeners.toList().forEach { emitter ->
                try {
                    emitter.emit(event)
                } catch (e: Exception) {
                }
            }
        }
    }

    fun notifyMatchStarted(
        lobbyId: Int,
        matchId: Int,
    ) {
        notifyLobby(lobbyId, LobbyEvent.MatchStarted(lobbyId, matchId))
    }

    // --- NOVA FUNÇÃO ---
    // Permite ao Controller saber se já existe uma partida para este lobby
    fun getMatchIdForLobby(lobbyId: Int): Int? =
        transactionManager.run {
            it.matchRepository.getMatchByLobbyId(lobbyId)?.matchId
        }
    // -------------------

    fun createLobby(
        name: String,
        isPublic: Boolean,
        host: User, // Recebe o User inteiro em vez de apenas hostId
        rounds: Int,
        expectedPlayers: Int,
        ante: Int,
        timeout: Long,
    ): Either<LobbyError, Lobby> =
        lock.withLock {
            // Validação de saldo
            if (host.balance < ante) {
                return failure(LobbyError.InsufficientFunds)
            }

            transactionManager.run {
                val lobbyRepository = it.lobbyRepository
                if (lobbyRepository.getLobbyByName(name) != null) {
                    failure(LobbyError.LobbyAlreadyExists)
                } else {
                    val lobby = lobbyRepository.createLobby(name, isPublic, host.id, rounds, expectedPlayers, ante, timeout)
                    success(lobby)
                }
            }
        }

    fun getLobbyById(id: Int): Either<LobbyError, Lobby> =
        transactionManager.run {
            val lobby = it.lobbyRepository.getLobbyById(id)
                ?: return@run failure(LobbyError.LobbyNotFound)
            success(lobby)
        }

    fun listAvailableLobbies(page: Int = 0, pageSize: Int = 20): Either<LobbyError, PaginatedLobbies> =
        transactionManager.run {
            val allLobbies = it.lobbyRepository.listAvailableLobbies()

            if (allLobbies.isEmpty()) {
                return@run failure(LobbyError.NoLobbiesExist)
            }

            val startIndex = page * pageSize
            if (startIndex >= allLobbies.size) {
                return@run failure(LobbyError.NoLobbiesExist)
            }

            val endIndex = min(startIndex + pageSize, allLobbies.size)
            val paginatedLobbies = allLobbies.subList(startIndex, endIndex)

            success(PaginatedLobbies(
                items = paginatedLobbies,
                total = allLobbies.size
            ))
        }

    fun joinLobby(
        lobbyId: Int,
        user: User,
    ): Either<LobbyError, Unit> =
        lock.withLock {
            val joinResult = transactionManager.run {
                val lobby = it.lobbyRepository.getLobbyById(lobbyId)
                    ?: return@run failure(LobbyError.LobbyNotFound)

                if (lobbyDomain.isPlayerInLobby(lobby, user.id)) {
                    return@run failure(LobbyError.AlreadyInLobby)
                }

                // VALIDAÇÃO DE SALDO ADICIONADA
                if (user.balance < lobby.ante) {
                    return@run failure(LobbyError.InsufficientFunds)
                }

                try {
                    lobbyDomain.addPlayer(lobby, user)
                } catch (e: IllegalArgumentException) {
                    return@run failure(LobbyError.LobbyFull)
                }

                it.lobbyRepository.addPlayerToLobby(lobbyId, user.id)
                notifyLobby(lobbyId, LobbyEvent.PlayerJoined(lobbyId, user.id, user.name))
                success(Unit)
            }

            if (joinResult is Either.Right) {
                scheduler.schedule({
                    try {
                        val startMatchResult = matchServices.tryAutoStartMatch(lobbyId)
                        when (startMatchResult) {
                            is Either.Right -> notifyMatchStarted(lobbyId, startMatchResult.value.matchId)
                            is Either.Left -> {
                                if (startMatchResult.value is MatchError.MatchAlreadyStarted) {
                                    val existingMatchId = getMatchIdForLobby(lobbyId)
                                    if (existingMatchId != null) notifyMatchStarted(lobbyId, existingMatchId)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, 3, TimeUnit.SECONDS)
            }

            joinResult
        }

    fun leaveLobby(
        lobbyId: Int,
        userId: Int,
    ): Either<LobbyError, Unit> =
        lock.withLock {
            transactionManager.run {
                val lobby = it.lobbyRepository.getLobbyById(lobbyId)
                    ?: return@run failure(LobbyError.LobbyNotFound)

                if (!lobbyDomain.isPlayerInLobby(lobby, userId)) {
                    return@run failure(LobbyError.NotInLobby)
                }

                val username = lobby.players.find { it.id == userId }?.name ?: "Jogador"

                if (lobby.host.id == userId) {
                    lobby.players.forEach { player ->
                        if (player.id != userId) {
                            it.lobbyRepository.removePlayerFromLobby(lobbyId, player.id)
                            notifyPlayerLobbyClosed(player.id, lobbyId)
                        }
                    }
                    it.lobbyRepository.deleteLobby(lobbyId)
                } else {
                    it.lobbyRepository.removePlayerFromLobby(lobbyId, userId)
                }

                notifyLobby(lobbyId, LobbyEvent.PlayerLeft(lobbyId, userId, username))
                success(Unit)
            }
        }

    fun deleteLobby(lobbyId: Int, userId: Int): Either<LobbyError, Unit> =
        transactionManager.run {
            val lobby = it.lobbyRepository.getLobbyById(lobbyId)
                ?: return@run failure(LobbyError.LobbyNotFound)

            // Verifica se quem está a pedir para apagar é o host
            if (lobby.host.id != userId) {
                return@run failure(LobbyError.NotLobbyHost)
            }

            it.lobbyRepository.deleteLobby(lobbyId)
            success(Unit)
        }

    private fun notifyPlayerLobbyClosed(playerId: Int, lobbyId: Int) {
        println("A notificar o jogador $playerId: O Lobby $lobbyId foi fechado")
    }
}