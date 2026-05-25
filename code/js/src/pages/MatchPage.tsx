import React, { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { matchService, type MatchState } from '../services/api/matches';
import { useAuthentication } from '../providers/AuthenticationProvider';

const styles = {
    container: {
        padding: '20px',
        color: 'white',
        maxWidth: '800px',
        margin: '80px auto',
        fontFamily: 'Arial, sans-serif'
    },
    header: {
        textAlign: 'center' as const,
        marginBottom: '20px',
        borderBottom: '1px solid #444',
        paddingBottom: '20px'
    },
    diceContainer: {
        display: 'flex',
        gap: '15px',
        justifyContent: 'center',
        margin: '30px 0'
    },
    die: (held: boolean, interactable: boolean) => ({
        width: '60px',
        height: '60px',
        backgroundColor: held ? '#007bff' : 'white',
        color: held ? 'white' : 'black',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        fontSize: '24px',
        fontWeight: 'bold' as const,
        cursor: interactable ? 'pointer' : 'default',
        borderRadius: '8px',
        border: held ? '4px solid #0056b3' : '2px solid #000',
        userSelect: 'none' as const,
        transition: 'all 0.2s'
    }),
    controls: {
        display: 'flex',
        gap: '20px',
        justifyContent: 'center',
        marginBottom: '40px'
    },
    btnAction: {
        padding: '12px 24px',
        fontSize: '16px',
        backgroundColor: '#007bff',
        color: 'white',
        border: 'none',
        borderRadius: '6px',
        cursor: 'pointer',
        fontWeight: 'bold' as const
    },
    btnStop: {
        padding: '12px 24px',
        fontSize: '16px',
        backgroundColor: 'black',
        color: 'white',
        border: '1px solid #444',
        borderRadius: '6px',
        cursor: 'pointer',
        fontWeight: 'bold' as const
    },


    btnBack: {
        padding: '10px 20px',
        fontSize: '16px',
        backgroundColor: '#007bff',
        color: 'white',
        border: 'none',
        borderRadius: '6px',
        cursor: 'pointer',
        fontWeight: 'bold' as const,
        marginTop: '10px'
    },
    scoreList: {
        display: 'flex',
        flexDirection: 'column' as const,
        gap: '10px'
    },
    playerCard: (isActive: boolean) => ({
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: '15px',
        backgroundColor: isActive ? '#1a1a1a' : '#2d2d2d',
        border: isActive ? '2px solid #007bff' : '1px solid #444',
        borderRadius: '8px'
    }),
    modalOverlay: {
        position: 'fixed' as const, top: 0, left: 0, right: 0, bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.9)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000
    },
    modalContent: {
        backgroundColor: 'white', color: 'black', padding: '30px', borderRadius: '15px', textAlign: 'center' as const,
        border: '4px solid #007bff', maxWidth: '400px', width: '90%'
    },
    modalBtn: {
        marginTop: '20px', padding: '10px 20px', backgroundColor: '#007bff', color: 'white',
        border: 'none', borderRadius: '5px', cursor: 'pointer', fontWeight: 'bold' as const, fontSize: '16px'
    },
    lastRoundInfo: {
        backgroundColor: '#222',
        border: '1px solid #007bff',
        padding: '15px',
        borderRadius: '8px',
        marginBottom: '20px',
        textAlign: 'center' as const
    }
};

export const MatchPage: React.FC = () => {
    const { matchId } = useParams<{ matchId: string }>();
    const navigate = useNavigate();
    const [matchState, setMatchState] = useState<MatchState | null>(null);
    const [user] = useAuthentication();

    const [currentDice, setCurrentDice] = useState<string[]>(['?', '?', '?', '?', '?']);
    const [rollsLeft, setRollsLeft] = useState(3);
    const [heldDice, setHeldDice] = useState<number[]>([]);

    const [winnerInfo, setWinnerInfo] = useState<{ name: string, pot: number, round: number, isGameOver: boolean } | null>(null);

    const prevRoundRef = useRef<number>(0);
    const prevPlayerRef = useRef<number>(0);
    const prevStatusRef = useRef<string>('NOT_STARTED');

    const fetchMatchState = async () => {
        if (!matchId) return;
        try {
            const result = await matchService.getMatchState(Number(matchId));
            if (result.success) {
                setMatchState(result.value);
            }
        } catch (error) { console.error(error); }
    };

    useEffect(() => {
        fetchMatchState();
        const interval = setInterval(fetchMatchState, 2000);
        return () => clearInterval(interval);
    }, [matchId]);

    useEffect(() => {
        if (!matchState) return;

        if (matchState.status === 'FINISHED' && prevStatusRef.current !== 'FINISHED') {
            console.log("🏁 Partida Finalizada!");

            const roundsWithWinner = matchState.rounds?.filter((r: any) => r.winnerId);
            const lastWinnerRound = roundsWithWinner?.sort((a: any, b: any) => b.roundNumber - a.roundNumber)[0];

            if (lastWinnerRound) {
                const winner = matchState.players.find(p => p.id === lastWinnerRound.winnerId);
                if (winner) {
                    setWinnerInfo({
                        name: winner.name,
                        pot: lastWinnerRound.pot,
                        round: lastWinnerRound.roundNumber,
                        isGameOver: true
                    });
                }
            }
        }

        else if (matchState.currentRound > prevRoundRef.current && prevRoundRef.current > 0) {
            const prevRoundData = matchState.rounds?.find((r: any) => r.roundNumber === prevRoundRef.current);
            if (prevRoundData && prevRoundData.winnerId) {
                const winner = matchState.players.find(p => p.id === prevRoundData.winnerId);
                if (winner) {
                    setWinnerInfo({
                        name: winner.name,
                        pot: prevRoundData.pot,
                        round: prevRoundData.roundNumber,
                        isGameOver: false
                    });
                }
            }
            setRollsLeft(3);
            setHeldDice([]);
            setCurrentDice(['?', '?', '?', '?', '?']);
        }

        else if (matchState.currentPlayerId !== prevPlayerRef.current) {
            setRollsLeft(3);
            setHeldDice([]);
            setCurrentDice(['?', '?', '?', '?', '?']);
        }

        prevRoundRef.current = matchState.currentRound;
        prevPlayerRef.current = matchState.currentPlayerId;
        prevStatusRef.current = matchState.status;

    }, [matchState]);

    const handleRoll = async () => {
        if (!matchState || !matchId) return;
        try {
            const result = await matchService.playTurn(Number(matchId), matchState.currentRound, heldDice);
            if (result.success) {
                setCurrentDice(result.value.dice);
                setRollsLeft(result.value.rollsLeft);
            } else { alert("Erro: " + result.error); }
        } catch (error) { console.error(error); }
    };

    const handleEndTurn = async () => {
        if (!matchState || !matchId) return;
        try {
            const result = await matchService.endTurn(Number(matchId), matchState.currentRound);
            if (result.success) {
                setRollsLeft(0);
                fetchMatchState();
            } else { alert(result.error); }
        } catch (error) { console.error(error); }
    };

    const toggleHold = (index: number) => {
        if (heldDice.includes(index)) setHeldDice(heldDice.filter(i => i !== index));
        else setHeldDice([...heldDice, index]);
    };

    if (!matchState) return <div style={{ color: 'white', textAlign: 'center', marginTop: '100px' }}>A carregar jogo...</div>;

    const currentRoundData = matchState.rounds?.find((r: any) => r.roundNumber === matchState.currentRound);
    const activePlayerId = currentRoundData ? currentRoundData.currentPlayerId : null;
    const activePlayer = matchState.players.find(p => p.id === activePlayerId);

    const isGameOver = matchState.status === 'FINISHED';
    const isMyTurn = !isGameOver && activePlayer?.name === user;
    const activePlayerName = activePlayer ? activePlayer.name : "A processar...";

    const getHandDisplay = (pid: number) => {
        const roundToShow = currentRoundData || matchState.rounds?.slice(-1)[0];
        if (!roundToShow || !roundToShow.turns) return "-";

        const turn = roundToShow.turns.find((t: any) => t.playerId === pid);
        if (turn) {
            return (
                <span>
                    <span style={{ color: '#007bff', fontWeight: 'bold' }}>{turn.finalHand}</span>
                    <span style={{ color: '#aaa', marginLeft: '8px' }}>(Pontuação: {turn.score})</span>
                </span>
            );
        }
        return "-";
    };

    let finalRoundWinnerDisplay = null;
    if (isGameOver) {
        const roundsWithWinner = matchState.rounds?.filter((r: any) => r.winnerId);
        const lastWinnerRound = roundsWithWinner?.sort((a: any, b: any) => b.roundNumber - a.roundNumber)[0];
        if (lastWinnerRound) {
            const winner = matchState.players.find(p => p.id === lastWinnerRound.winnerId);
            finalRoundWinnerDisplay = (
                <div style={styles.lastRoundInfo}>
                    <h3>🏆 Vencedor da Ronda Final ({lastWinnerRound.roundNumber})</h3>
                    <p style={{fontSize: '1.2em'}}><strong>{winner?.name}</strong> ganhou <strong>{lastWinnerRound.pot}</strong> créditos</p>
                </div>
            );
        }
    }

    return (
        <div style={styles.container}>

            {winnerInfo && (
                <div style={styles.modalOverlay}>
                    <div style={styles.modalContent}>
                        <h2 style={{ color: 'black', marginBottom: '10px' }}>
                            {winnerInfo.isGameOver ? "🏁 Jogo Terminado!" : `Ronda ${winnerInfo.round} Finalizada`}
                        </h2>

                        <p style={{ fontSize: '1.2em' }}>
                            Vencedor da Ronda: <strong>{winnerInfo.name}</strong>
                        </p>

                        <p style={{ fontSize: '1.5em', color: '#007bff', fontWeight: 'bold', margin: '20px 0' }}>
                            +{winnerInfo.pot} Créditos
                        </p>

                        <button
                            style={styles.modalBtn}
                            onClick={() => {
                                if (winnerInfo.isGameOver) navigate('/lobbies');
                                else setWinnerInfo(null);
                            }}
                        >
                            {winnerInfo.isGameOver ? "Sair da Partida" : "Continuar"}
                        </button>
                    </div>
                </div>
            )}

            <div style={styles.header}>
                <h1 style={{margin:0}}>Mesa #{matchId}</h1>
                <h3 style={{color: '#aaa', marginTop:'5px'}}>
                    {isGameOver ? "RESULTADOS FINAIS" : `Ronda ${matchState.currentRound} / ${matchState.totalRounds}`}
                </h3>

                <div style={{ fontSize: '1.2em', marginTop: '15px' }}>
                    {isGameOver ? (
                        <div>
                            <span style={{ color: '#007bff', fontWeight: 'bold' }}>🏁 Partida Encerrada</span>
                            <div style={{marginTop: '15px'}}>
                                <button style={styles.btnBack} onClick={() => navigate('/lobbies')}>
                                    ⬅️ Voltar ao Menu
                                </button>
                            </div>
                        </div>
                    ) : isMyTurn ? (
                        <span style={{ color: '#007bff', fontWeight: 'bold' }}>⚡ É A TUA VEZ!</span>
                    ) : (
                        <span style={{ color: '#ccc' }}>À espera de: <strong>{activePlayerName}</strong></span>
                    )}
                </div>
            </div>

            {isGameOver && finalRoundWinnerDisplay}

            {!isGameOver && (
                <>
                    <div style={styles.diceContainer}>
                        {currentDice.map((face, index) => (
                            <div
                                key={index}
                                onClick={() => isMyTurn && rollsLeft < 3 && rollsLeft > 0 ? toggleHold(index) : null}
                                style={styles.die(heldDice.includes(index), isMyTurn && rollsLeft < 3)}
                            >
                                {face}
                            </div>
                        ))}
                    </div>
                    <p style={{ textAlign: 'center', marginBottom: '20px' }}>
                        Lançamentos: <strong>{rollsLeft}</strong>
                    </p>
                </>
            )}

            {!isGameOver && isMyTurn && (
                <div style={styles.controls}>
                    <button
                        onClick={handleRoll}
                        disabled={rollsLeft <= 0}
                        style={{...styles.btnAction, opacity: rollsLeft <= 0 ? 0.5 : 1}}
                    >
                        🎲 Lançar Dados
                    </button>

                    <button onClick={handleEndTurn} style={styles.btnStop}>
                        🛑 Finalizar
                    </button>
                </div>
            )}

            <div style={styles.scoreList}>
                <h3 style={{ borderBottom: '1px solid #555', paddingBottom: '10px' }}>Jogadores</h3>
                {matchState.players.map(p => {
                    const isActive = !isGameOver && p.id === activePlayerId;
                    const isMe = p.name === user;

                    return (
                        <div key={p.id} style={styles.playerCard(isActive)}>
                            <div style={{ minWidth: '120px' }}>
                                <span style={{ fontSize: '1.2em', fontWeight: 'bold', color: isActive ? '#007bff' : 'white' }}>
                                    {p.name} {isMe ? '(Tu)' : ''}
                                </span>
                                {isActive && <span style={{ marginLeft: '10px' }}>🎲</span>}
                            </div>

                            <div style={{ textAlign: 'center', flexGrow: 1 }}>
                                <div style={{ color: '#555', fontSize: '0.8em', textTransform: 'uppercase' }}>
                                    {isGameOver ? "Mão Final" : "Mão Atual"}
                                </div>
                                <div>{getHandDisplay(p.id)}</div>
                            </div>

                            <div style={{ textAlign: 'right', minWidth: '80px' }}>
                                <div style={{ color: '#555', fontSize: '0.8em', textTransform: 'uppercase' }}>Saldo</div>
                                <div style={{ fontSize: '1.4em', color: isMe ? '#007bff' : '#555', fontWeight: 'bold' }}>
                                    {isMe ? p.balance : '???'}
                                </div>
                            </div>
                        </div>
                    );
                })}
            </div>
        </div>
    );
};