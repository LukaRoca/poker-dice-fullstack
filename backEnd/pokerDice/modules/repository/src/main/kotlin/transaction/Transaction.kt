package transaction

import transaction.repository.LobbyRepository
import transaction.repository.MatchRepository
import transaction.repository.UserRepository

interface Transaction {
    val usersRepository: UserRepository

    val matchRepository: MatchRepository

    val lobbyRepository: LobbyRepository

    fun rollback()
}
