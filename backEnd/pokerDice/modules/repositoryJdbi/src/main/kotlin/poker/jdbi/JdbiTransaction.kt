package poker.jdbi

import org.jdbi.v3.core.Handle
import transaction.Transaction
import transaction.repository.LobbyRepository
import transaction.repository.MatchRepository
import transaction.repository.UserRepository

class JdbiTransaction(
    private val handle: Handle,
) : Transaction {
    override val usersRepository: UserRepository = JdbiUserRepository(handle)
    override val matchRepository: MatchRepository = JdbiMatchRepository(handle)
    override val lobbyRepository: LobbyRepository = JdbiLobbyRepository(handle)

    override fun rollback() {
        handle.rollback()
    }
}
