import { fetchWrapper, type Result } from './utils';
import { ApiRoutes } from './requestUri';

export interface MatchState {
    matchId: number;
    lobbyId: number;
    currentRound: number;
    totalRounds: number;
    status: 'NOT_STARTED' | 'ONGOING' | 'FINISHED';
    currentPlayerId: number;
    players: MatchPlayer[];
    rounds: any[];
}

export interface MatchPlayer {
    id: number;
    name: string;
    balance: number;
}

export interface PlayTurnResponse {
    playerId: number;
    dice: string[];
    rollsLeft: number;
}

export const matchService = {
    getMatchState(matchId: number): Promise<Result<MatchState>> {
        return fetchWrapper(ApiRoutes.match.getState(matchId), {
            method: 'GET',
        });
    },

    playTurn(matchId: number, roundNumber: number, heldDiceIndices: number[]): Promise<Result<PlayTurnResponse>> {
        return fetchWrapper<PlayTurnResponse>(ApiRoutes.match.play(matchId, roundNumber), {
            method: 'POST',
            body: JSON.stringify({ heldIndices: heldDiceIndices }),
        });
    },

    endTurn(matchId: number, roundNumber: number): Promise<Result<any>> {
        return fetchWrapper(ApiRoutes.match.endTurn(matchId, roundNumber), {
            method: 'POST'
        });
    }
};