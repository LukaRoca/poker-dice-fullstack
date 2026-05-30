package poker

import jakarta.inject.Named
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import poker.events.GlobalEvent
import poker.lobby.Lobby
import poker.lobby.LobbyDomain
import poker.match.Match
import poker.match.MatchDomain
import poker.match.MatchStatus
import poker.utils.Either
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

@Named
class LobbyLifecycleService(
    private val lobbyServices: LobbyServices,
    private val matchService: MatchServices,
    private val lobbyDomain: LobbyDomain,
    private val matchDomain: MatchDomain,
    private val notificationService: NotificationService,
    private val clock: Clock = Clock.System
) : NeedsShutdown {

    companion object {
        private val logger = LoggerFactory.getLogger(LobbyLifecycleService::class.java)
    }

    private val scheduler: ScheduledExecutorService =
        Executors.newScheduledThreadPool(1).also {
            it.scheduleAtFixedRate({ executeTasks() }, 5, 5, TimeUnit.SECONDS)
        }

    override fun shutdown() {
        logger.info("A desligar o LobbyLifecycleService...")
        scheduler.shutdown()
    }

    private fun executeTasks() {
        try {
            checkOpenLobbies()
            checkActiveMatches()
        } catch (e: Exception) {
            logger.error("Erro na tarefa agendada de ciclo de vida de jogos", e)
        }
    }

    private fun checkOpenLobbies() {
        val result = lobbyServices.listAvailableLobbies(0, 1000)

        if (result is Either.Right) {
            val lobbies = result.value.items
            lobbies.forEach { lobby ->
                processLobby(lobby)
            }
        }
    }

    private fun processLobby(lobby: Lobby) {
        val now = clock.now()
        val deadline = lobby.createdAt + lobby.timeout
        val hasExpired = now >= deadline
        val playerCount = lobby.players.size

        if (!hasExpired && playerCount < lobby.expectedPlayers.count) {
            return
        }

        if (lobbyDomain.canStartMatch(lobby, now)) {
            logger.info("A iniciar match automático para o Lobby ${lobby.id.id}")

            val startResult = matchService.tryAutoStartMatch(lobby.id.id)

            if (startResult is Either.Right) {
                val match = startResult.value

                // 1. Notifica o canal do Lobby
                lobbyServices.notifyMatchStarted(lobby.id.id, match.matchId)

                // 2. Garante que o HOST e os JOGADORES são notificados
                val usersToNotify = (lobby.players + lobby.host).distinctBy { it.id }

                logger.info("📢 A notificar ${usersToNotify.size} utilizadores do início da partida ${match.matchId}: ${usersToNotify.map { it.name }}")

                usersToNotify.forEach { user ->
                    try {
                        notificationService.sendEventToUser(
                            user.id,
                            GlobalEvent.MatchStarted(match.matchId)
                        )
                    } catch (e: Exception) {
                        logger.error("Falha ao notificar utilizador ${user.name} (${user.id})", e)
                    }
                }

            } else {
                logger.warn("Erro ao iniciar match automático: $startResult")
            }
        }
        else if (hasExpired && playerCount < 2) {
            logger.info("O Lobby ${lobby.id.id} expirou sem jogadores suficientes. A apagar...")

            // CORREÇÃO: Passamos lobby.host.id para autorizar a remoção pelo sistema
            val deleteResult = lobbyServices.deleteLobby(lobby.id.id, lobby.host.id)

            if (deleteResult is Either.Left) {
                logger.error("Falha ao apagar lobby expirado ${lobby.id.id}: $deleteResult")
            }
        }
    }

    private fun checkActiveMatches() {
        val activeMatchesResult = matchService.listOngoingMatches()

        if (activeMatchesResult is Either.Right) {
            activeMatchesResult.value.forEach { match ->
                processMatchForNextRound(match)
            }
        }
    }

    private fun processMatchForNextRound(match: Match) {
        val lastRound = match.rounds.lastOrNull()

        if (lastRound != null && match.status == MatchStatus.ONGOING) {

            if (lastRound.currPlayerId == null && lastRound.winnerId == null) {
                logger.info("Ronda ${lastRound.roundNumber} do Match ${match.matchId} completa. A resolver fim de ronda...")

                val endRoundResult = matchService.tryAutoEndRound(match.matchId, lastRound.roundNumber)

                if (endRoundResult is Either.Right) {
                    logger.info("Ronda ${lastRound.roundNumber} resolvida. Pot distribuído.")
                } else if (endRoundResult is Either.Left) {
                    logger.error("Falha ao resolver ronda ${lastRound.roundNumber} do Match ${match.matchId}: $endRoundResult")
                }
            }
            if (lastRound.winnerId != null && !matchDomain.isMatchFinished(match)) {
                logger.info("Match ${match.matchId} resolvido. A iniciar próxima ronda...")

                val startRoundResult = matchService.tryAutoStartNewRound(match.matchId)

                if (startRoundResult is Either.Left && startRoundResult.value is MatchError.MatchAlreadyStarted) {
                    logger.info("Match ${match.matchId} finalizado após a ronda ${lastRound.roundNumber}.")
                } else if (startRoundResult is Either.Left) {
                    logger.error("Falha ao iniciar próxima ronda para Match ${match.matchId}: $startRoundResult")
                }
            }
        }
    }
}