import { useEffect } from 'react';
import { useSSEEmitter } from '../providers/SSEContext';
import type { PlayerJoinedEvent, PlayerLeftEvent, MatchStartedEvent } from '../components/models/MatchEvent';

type LobbyEventHandlers = {
    onPlayerJoined?: (data: PlayerJoinedEvent) => void;
    onPlayerLeft?: (data: PlayerLeftEvent) => void;
    onLobbyClosed?: () => void;
    onMatchStarted?: (data: MatchStartedEvent) => void;
}

export function useLobbyEventSubscription(handlers: LobbyEventHandlers) {
    const { isSSEConnected, registerHandler, unregisterHandler } = useSSEEmitter();

    const { onPlayerJoined, onPlayerLeft, onLobbyClosed, onMatchStarted } = handlers;

    useEffect(() => {
        if (!isSSEConnected) return;

        const eventRouter = (data: any) => {

            switch (data.type) {
                case 'player_joined':
                    onPlayerJoined?.(data as PlayerJoinedEvent);
                    break;
                case 'player_left':
                    onPlayerLeft?.(data as PlayerLeftEvent);
                    break;
                case 'lobby_closed':
                    onLobbyClosed?.();
                    break;
                case 'match_started':
                    onMatchStarted?.(data as MatchStartedEvent);
                    break;
                default:
                    console.warn("Evento desconhecido:", data.type);
                    break;
            }
        };

        registerHandler('player_joined', eventRouter);
        registerHandler('player_left', eventRouter);
        registerHandler('lobby_closed', eventRouter);
        registerHandler('match_started', eventRouter);

        return () => {
            unregisterHandler('player_joined');
            unregisterHandler('player_left');
            unregisterHandler('lobby_closed');
            unregisterHandler('match_started');
        };

    }, [isSSEConnected, registerHandler, unregisterHandler,
        onPlayerJoined, onPlayerLeft,
        onLobbyClosed, onMatchStarted
    ]);
}