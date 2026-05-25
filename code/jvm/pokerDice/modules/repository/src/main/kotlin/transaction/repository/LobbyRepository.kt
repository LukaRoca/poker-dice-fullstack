package transaction.repository

import poker.lobby.Lobby

interface LobbyRepository {
    fun createLobby(
        name: String,
        isPublic: Boolean,
        hostId: Int,
        rounds: Int,
        expectedPlayers: Int,
        ante: Int,
        timeout: Long,
    ): Lobby

    fun getLobbyById(id: Int): Lobby?

    fun listAvailableLobbies(): List<Lobby>

    fun addPlayerToLobby(
        lobbyId: Int,
        userId: Int,
    )

    fun removePlayerFromLobby(
        lobbyId: Int,
        userId: Int,
    )

    fun deleteLobby(lobbyId: Int)

    fun getLobbyByName(name: String): Lobby?

    fun updateLobbyState(
        lobbyId: Int,
        state: String,
    )
}
