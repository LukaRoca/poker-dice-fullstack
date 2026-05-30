import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuthentication } from '../../providers/AuthenticationProvider';
import { NavBarBase, AuthenticationNavBar } from '../layout/NavBar';
import { fetchWrapper } from '../../services/api/utils';

interface Lobby {
    id: number;
    name: string;
    hostUsername: string;
    currentPlayerCount: number;
    expectedPlayers: number;
    state: string;
}

export const LobbiesPage: React.FC = () => {
    const [user] = useAuthentication();
    const [lobbies, setLobbies] = useState<Lobby[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [currentPage, setCurrentPage] = useState(0);
    const [pageSize] = useState(5);
    const [totalPages, setTotalPages] = useState(1);

    useEffect(() => {
        const fetchData = async () => {
            try {
                setLoading(true);
                setError(null);

                const result = await fetchWrapper<any>(`/api/lobbies?page=${currentPage}&pageSize=${pageSize}`);

                if (result.success) {
                    const responseData = result.value;
                    const lobbiesData = responseData.items || responseData.lobbies || [];
                    setLobbies(lobbiesData);
                    setTotalPages(responseData.totalPages || 1);
                } else {
                    if (result.error.includes("404") || result.error.includes("Not Found")) {
                        setLobbies([]);
                        setTotalPages(1);
                    } else {
                        setError(result.error);
                    }
                }

            } catch (err) {
                console.error('Erro ao carregar dados:', err);
            } finally {
                setLoading(false);
            }
        };

        if (user) {
            fetchData();
        }
    }, [user, currentPage, pageSize]);

    const handleDeleteLobby = async (lobbyId: number, lobbyName: string) => {
        if (!window.confirm(`Tens a certeza que queres apagar o lobby "${lobbyName}"?`)) {
            return;
        }

        try {
            const result = await fetchWrapper(`/api/lobbies/${lobbyId}/delete`, {
                method: 'DELETE'
            });

            if (result.success) {
                if (lobbies.length === 1 && currentPage > 0) {
                    setCurrentPage(prev => prev - 1);
                }
                else {
                    setLobbies(prev => prev.filter(lobby => lobby.id !== lobbyId));
                }
            } else {
                setError(result.error);
            }
        } catch (err) {
            setError('Erro ao apagar lobby');
        }
    };

    const isUserHost = (lobbyHostUsername: string) => {
        return user && user === lobbyHostUsername;
    };

    if (!user) {
        return (
            <div style={{ padding: '20px', textAlign: 'center', color: 'white' }}>
                <h1>Precisa de fazer login para ver os lobbies</h1>
                <Link to="/login" className="nav-btn btn-login">
                    Fazer Login
                </Link>
            </div>
        );
    }

    return (
        <div className="min-h-screen flex flex-col" style={{ backgroundColor: '#242424' }}>
            <NavBarBase>
                <AuthenticationNavBar name={user} />
            </NavBarBase>

            <div style={{ padding: '20px', maxWidth: '1200px', margin: '0 auto', width: '100%', marginTop: '80px' }}>
                <div style={{ marginBottom: '30px', textAlign: 'center' }}>
                    <h1 style={{ fontSize: '2.5em', color: 'white', marginBottom: '10px' }}>
                        Lobbies Disponíveis
                    </h1>
                    <p style={{ color: '#ccc', fontSize: '1.1em' }}>
                        Lista de partidas à espera de jogadores. Clique num lobby para entrar.
                    </p>
                </div>

                <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '30px' }}>
                    <Link
                        to="/lobbies/create"
                        className="nav-btn btn-register"
                        style={{ fontSize: '1.1em', padding: '12px 30px' }}
                    >
                        🎮 Criar Novo Lobby
                    </Link>
                </div>

                {loading && (
                    <div style={{ textAlign: 'center', color: '#ccc', fontSize: '1.2em' }}>
                        A carregar lobbies...
                    </div>
                )}

                {error && (
                    <div style={{
                        backgroundColor: '#dc3545',
                        color: 'white',
                        padding: '15px',
                        borderRadius: '5px',
                        marginBottom: '20px',
                        textAlign: 'center'
                    }}>
                        {error}
                    </div>
                )}

                {!loading && lobbies.length === 0 && (
                    <div style={{
                        backgroundColor: '#2d2d2d',
                        color: '#ccc',
                        padding: '40px',
                        borderRadius: '10px',
                        textAlign: 'center'
                    }}>
                        <h3 style={{ color: '#fff', marginBottom: '15px' }}>Nenhum lobby disponível</h3>
                        <p>Seja o primeiro a criar um lobby e convide os seus amigos!</p>
                    </div>
                )}

                {lobbies.length > 0 && (
                    <>
                        <div style={{ display: 'grid', gap: '15px', marginBottom: '20px' }}>
                            {lobbies.map(lobby => (
                                <div
                                    key={lobby.id}
                                    style={{
                                        backgroundColor: '#2d2d2d',
                                        padding: '20px',
                                        borderRadius: '10px',
                                        border: '1px solid #444',
                                        color: 'white'
                                    }}
                                >
                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                        <div>
                                            <h3 style={{ color: '#007bff', margin: '0 0 10px 0' }}>
                                                {lobby.name}
                                                {isUserHost(lobby.hostUsername) && (
                                                    <span style={{
                                                        marginLeft: '10px',
                                                        fontSize: '0.8em',
                                                        backgroundColor: '#ffffff',
                                                        color: 'white',
                                                        padding: '2px 8px',
                                                        borderRadius: '12px'
                                                    }}>
                                                    </span>
                                                )}
                                            </h3>
                                            <div style={{ display: 'flex', gap: '20px', fontSize: '0.9em' }}>
                                                <span>👤 Host: {lobby.hostUsername}</span>
                                                <span>🎯 Jogadores: {lobby.currentPlayerCount}/{lobby.expectedPlayers}</span>
                                                <span>📊 Estado: {lobby.state === 'OPEN' ? 'Aberto' : lobby.state === 'FULL' ? 'Cheio' : 'Fechado'}</span>
                                            </div>
                                        </div>

                                        <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                                            <Link
                                                to={`/lobby/${lobby.id}`}
                                                className="nav-btn btn-register"
                                                style={{
                                                    padding: '10px 20px',
                                                    textDecoration: 'none',
                                                    display: 'inline-block',
                                                    textAlign: 'center'
                                                }}
                                            >
                                                Ver
                                            </Link>

                                            {isUserHost(lobby.hostUsername) && (
                                                <button
                                                    onClick={() => handleDeleteLobby(lobby.id, lobby.name)}
                                                    style={{
                                                        padding: '10px 20px',
                                                        backgroundColor: '#dc3545',
                                                        color: 'white',
                                                        border: 'none',
                                                        borderRadius: '5px',
                                                        cursor: 'pointer'
                                                    }}
                                                >
                                                    Apagar
                                                </button>
                                            )}
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>

                        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '20px', marginTop: '30px' }}>
                            <button
                                onClick={() => setCurrentPage(prev => Math.max(0, prev - 1))}
                                disabled={currentPage === 0}
                                style={{
                                    padding: '10px 20px',
                                    backgroundColor: currentPage === 0 ? '#6c757d' : '#007bff',
                                    color: 'white',
                                    border: 'none',
                                    borderRadius: '5px',
                                    cursor: currentPage === 0 ? 'not-allowed' : 'pointer',
                                    opacity: currentPage === 0 ? 0.5 : 1
                                }}
                            >
                                Anterior
                            </button>

                            <span style={{ color: 'white' }}>
                                Página {currentPage + 1} de {totalPages}
                            </span>

                            <button
                                onClick={() => setCurrentPage(prev => prev + 1)}
                                disabled={currentPage >= totalPages - 1}
                                style={{
                                    padding: '10px 20px',
                                    backgroundColor: currentPage >= totalPages - 1 ? '#6c757d' : '#007bff',
                                    color: 'white',
                                    border: 'none',
                                    borderRadius: '5px',
                                    cursor: currentPage >= totalPages - 1 ? 'not-allowed' : 'pointer',
                                    opacity: currentPage >= totalPages - 1 ? 0.5 : 1
                                }}
                            >
                                Próxima
                            </button>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
};