import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, Navigate } from 'react-router-dom';
import { useAuthentication } from '../../providers/AuthenticationProvider';
import { NavBarBase, AuthenticationNavBar } from '../layout/NavBar';
import { fetchWrapper } from '../../services/api/utils';
import { useNotifications } from '../../providers/NotificationContext';

interface Player {
    id: number;
    name: string;
}

interface LobbyDetail {
    id: number;
    name: string;
    description: string;
    host: Player;
    rounds: number;
    ante: number;
    expectedPlayers: number;
    state: string;
    players: Player[];
    matchId?: number;
}

export const LobbyDetailPage: React.FC = () => {
    const { lobbyId } = useParams<{ lobbyId: string }>();
    const [user] = useAuthentication();
    const navigate = useNavigate();
    const [lobby, setLobby] = useState<LobbyDetail | null>(null);
    const [loading, setLoading] = useState(true);
    const [, setError] = useState<string | null>(null);

    const { addNotification } = useNotifications();

    const fetchLobby = async () => {
        if (!lobbyId) return;
        try {
            const result = await fetchWrapper<LobbyDetail>(`/api/lobbies/${lobbyId}`);
            if (result.success) {
                setLobby(result.value);
                if (result.value.matchId) {
                    navigate(`/match/${result.value.matchId}`);
                }
            } else {
                if (!result.error.includes("404")) {
                    setError(result.error);
                }
            }
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchLobby();
        const interval = setInterval(fetchLobby, 2000);
        return () => clearInterval(interval);
    }, [lobbyId]);

    useEffect(() => {
        if (!lobbyId || !user) return;

        console.log(`A conectar ao Lobby ${lobbyId}...`);

        const url = `/api/lobbies/${lobbyId}/listen`;

        const eventSource = new EventSource(url, {
            withCredentials: true
        });

        eventSource.onopen = () => {
            console.log("Conexão SSE do Lobby Estabelecida!");
        };

        eventSource.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                console.log("📩 Evento recebido:", data);

                if (data.type === 'match_started' && data.matchId) {
                    addNotification("A partida começou!", "success");
                    console.log("🚀 PARTIDA COMEÇOU! A navegar para:", data.matchId);
                    navigate(`/match/${data.matchId}`);
                }

                if (data.type === 'player_joined') {
                    if (data.username) {
                        addNotification(`${data.username} entrou no lobby!`, "success");
                    }
                    fetchLobby();
                }

                if (data.type === 'player_left') {
                    if (data.username) {
                        addNotification(`${data.username} saiu do lobby.`, "warning");
                    }
                    fetchLobby();
                }

                if (data.type === 'lobby_closed') {
                    if (!data.matchId) {
                        addNotification("O host fechou o lobby", "error");
                        navigate('/lobbies');
                    }
                }

            } catch (e) {
                console.error("Erro ao ler evento:", e);
            }
        };

        eventSource.onerror = (err) => {
            console.warn("[Nativo] Aviso na conexão SSE do Lobby", err);
        };

        return () => {
            console.log("[Nativo] A fechar conexão...");
            eventSource.close();
        };
    }, [lobbyId, user, navigate, addNotification]);

    const handleJoinLobby = async () => {
        try {
            const result = await fetchWrapper(`/api/lobbies/${lobbyId}/join`, { method: 'POST' });
            if (result.success) fetchLobby();
            else alert(result.error);
        } catch (err) { alert('Erro ao entrar'); }
    };

    const handleLeaveLobby = async () => {
        if (!window.confirm("Tens a certeza que queres sair?")) return;
        try {
            const result = await fetchWrapper(`/api/lobbies/${lobbyId}/leave`, { method: 'POST' });
            if (result.success) navigate('/lobbies');
        } catch (err) { console.error(err); }
    };

    const handleDeleteLobby = async () => {
        if (!window.confirm("Atenção: És o Host. Isto vai apagar o lobby para todos. Continuar?")) return;
        try {
            const result = await fetchWrapper(`/api/lobbies/${lobbyId}`, { method: 'DELETE' });
            if (result.success) {
                navigate('/lobbies');
            } else {
                alert(result.error || "Erro ao apagar lobby");
            }
        } catch (err) { console.error(err); }
    };

    if (!user) return <Navigate to="/login" replace />;
    if (loading) return <div style={{color:'white', textAlign:'center', marginTop:'50px'}}>A carregar...</div>;

    if (!lobby && !loading) {
        return (
            <div style={{color:'white', textAlign:'center', marginTop:'50px'}}>
                <h1>A preparar partida...</h1>
                <p>Por favor aguarde...</p>
                <div style={{marginTop: '20px'}}>...</div>
            </div>
        );
    }

    if (!lobby) return null;

    const isHost = lobby.host.name === user;
    const isPlayer = lobby.players.some(p => p.name === user);

    return (
        <div className="min-h-screen flex flex-col" style={{ backgroundColor: '#242424' }}>
            <NavBarBase><AuthenticationNavBar name={user} /></NavBarBase>

            <div style={{ padding: '20px', maxWidth: '800px', margin: '80px auto', color: 'white' }}>
                <div style={{ textAlign: 'center', marginBottom: '30px' }}>
                    <h1 style={{ fontSize: '2.5em' }}>{lobby.name}</h1>
                    <p style={{ color: '#ccc' }}>Estado: {lobby.state}</p>
                </div>

                <div style={{ backgroundColor: '#2d2d2d', padding: '25px', borderRadius: '10px', marginBottom: '20px' }}>
                    <h2 style={{ color: '#007bff' }}>Jogadores ({lobby.players.length}/{lobby.expectedPlayers})</h2>
                    <span> Valor a apostar em cada ronda: {lobby.ante}</span>
                    {lobby.players.map(p => (
                        <div key={p.id} style={{ padding: '10px', borderBottom: '1px solid #444' }}>
                            {p.id === lobby.host.id ? '[Host] ' : ''}
                            {p.name} {p.name === user ? ' (Tu)' : ''}
                        </div>
                    ))}
                </div>

                <div style={{ display: 'flex', gap: '15px', justifyContent: 'center' }}>
                    <button onClick={() => navigate('/lobbies')} style={{ padding: '10px 20px', cursor:'pointer' }}>Voltar</button>

                    {isHost ? (
                        <button
                            onClick={handleDeleteLobby}
                            style={{ padding: '10px 20px', backgroundColor: '#dc3545', color:'white', cursor:'pointer', border:'none', borderRadius:'5px' }}
                        >
                            Apagar Lobby
                        </button>
                    ) : (
                        isPlayer ? (
                            <button onClick={handleLeaveLobby} style={{ padding: '10px 20px', backgroundColor: '#dc3545', color:'white', cursor:'pointer', border:'none', borderRadius:'5px' }}>Sair</button>
                        ) : (
                            <button onClick={handleJoinLobby} style={{ padding: '10px 20px', backgroundColor: '#28a745', color:'white', cursor:'pointer', border:'none', borderRadius:'5px' }}>Entrar</button>
                        )
                    )}
                </div>
            </div>
        </div>
    );
};