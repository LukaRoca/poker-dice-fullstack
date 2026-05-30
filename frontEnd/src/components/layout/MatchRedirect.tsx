import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useLobbyEventSubscription } from '../../hooks/useLobbyEvent';

export const GlobalMatchRedirect: React.FC = () => {
    const navigate = useNavigate();

    useLobbyEventSubscription({
        onMatchStarted: (data) => {
            console.log("[GlobalRedirect] Partida a começar! Redirecionando para:", data.matchId);

            if (data.matchId) {
                navigate(`/match/${data.matchId}`);
            }
        },
    });

    return null;
};