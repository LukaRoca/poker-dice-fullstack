export interface PlayerJoinedEvent {
    type: 'PlayerJoined';
    userId: number;
    name: string;
}

export interface PlayerLeftEvent {
    type: 'PlayerLeft';
    userId: number;
    name: string;
}
export interface MatchStartedEvent {
    type: 'MatchStarted';
    matchId: string;
}