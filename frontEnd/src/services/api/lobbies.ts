import { fetchWrapper, type Result } from "./utils.ts";
import { ApiRoutes } from "./requestUri.ts";

export interface CreateLobbyCredentials {
    name: string;
    isPublic: boolean;
    rounds: number;
    expectedPlayers: number;
    ante: number;
    timeout: number;
}

interface LobbyResponse {
    id: number;
    name: string;
    description: string;
    host: {
        id: number;
        name: string;
    };
    rounds: number;
    ante: number;
    expectedPlayers: number;
    state: string;
    players: Array<{
        id: number;
        name: string;
    }>;
}

export const lobbyService = {
    createLobby(credentials: CreateLobbyCredentials): Promise<Result<LobbyResponse>> {
        return fetchWrapper<LobbyResponse>(ApiRoutes.lobby.create, {
            method: 'POST',
            body: JSON.stringify(credentials)
        });
    },
};