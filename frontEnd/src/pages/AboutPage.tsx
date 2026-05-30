import React from 'react';
import { useNavigate } from "react-router-dom";
import {useAuthentication} from "../providers/AuthenticationProvider.tsx";
import {AuthenticationNavBar, NavBarBase, NoAuthenticationNavBar} from "../components/layout/NavBar.tsx";

export const AboutPage: React.FC = () => {
    const navigate = useNavigate();
    const [user] = useAuthentication();

    const handleCancel = () => {
        navigate(-1);
    };

    return (
        <div className="min-h-screen flex flex-col" style={{ backgroundColor: '#242424', color: 'white' }}>
            <NavBarBase>
                {user ? <AuthenticationNavBar name={user} /> : <NoAuthenticationNavBar />}
            </NavBarBase>

            <div style={{
                padding: '40px 20px',
                maxWidth: '1000px',
                margin: '80px auto 0 auto',
                width: '100%'
            }}>
                <div style={{ textAlign: 'center', marginBottom: '50px' }}>
                    <h1 style={{ fontSize: '3em', marginBottom: '20px', color: '#007bff' }}>
                        Poker Dice Game
                    </h1>
                    <p style={{ fontSize: '1.2em', color: '#ccc' }}>
                        Tutorial Completo - Como Jogar
                    </p>
                </div>

                <div style={{
                    backgroundColor: '#2d2d2d',
                    padding: '30px',
                    borderRadius: '10px',
                    marginBottom: '30px'
                }}>
                    <h2 style={{ color: '#007bff', marginBottom: '20px', fontSize: '1.8em' }}>
                        O que é o Poker Dice?
                    </h2>
                    <p style={{ lineHeight: '1.6', marginBottom: '15px' }}>
                        O Poker Dice é um jogo de dados baseado no poker tradicional, mas usando dados especiais
                        com faces de poker (A, K, Q, J, 10, 9) em vez de cartas.
                    </p>
                </div>
                <div style={{
                    backgroundColor: '#2d2d2d',
                    padding: '30px',
                    borderRadius: '10px',
                    marginBottom: '30px'
                }}>
                    <h2 style={{ color: '#007bff', marginBottom: '30px', fontSize: '1.8em' }}>
                        Como Jogar - Passo a Passo
                    </h2>

                    <div style={{ marginBottom: '25px' }}>
                        <h3 style={{ color: '#4CAF50', marginBottom: '15px', fontSize: '1.3em' }}>1. Criar ou Entrar num Lobby</h3>
                        <p style={{ lineHeight: '1.6', marginBottom: '10px' }}>
                            - Vai à página <strong>"Lobbies Disponíveis"</strong><br/>
                            - <strong>Cria um novo lobby</strong> ou <strong>entra num existente</strong><br/>
                            - Configura o número de jogadores e rondas
                        </p>
                    </div>

                    <div style={{ marginBottom: '25px' }}>
                        <h3 style={{ color: '#FF9800', marginBottom: '15px', fontSize: '1.3em' }}>2. Esperar pelos Jogadores</h3>
                        <p style={{ lineHeight: '1.6', marginBottom: '10px' }}>
                            - O lobby enche quando atinge o número máximo de jogadores<br/>
                            - Ou quando passa o tempo limite com jogadores mínimos<br/>
                            - Os jogadores podem entrar e sair livremente
                        </p>
                    </div>

                    <div style={{ marginBottom: '25px' }}>
                        <h3 style={{ color: '#9C27B0', marginBottom: '15px', fontSize: '1.3em' }}>3. Início da Partida</h3>
                        <p style={{ lineHeight: '1.6', marginBottom: '10px' }}>
                            - Cada jogador paga uma <strong>ante</strong> (aposta inicial)<br/>
                            - As rondas começam automaticamente<br/>
                            - O primeiro jogador a jogar alterna a cada ronda
                        </p>
                    </div>

                    <div style={{ marginBottom: '25px' }}>
                        <h3 style={{ color: '#E91E63', marginBottom: '15px', fontSize: '1.3em' }}>4. O Teu Turno</h3>
                        <p style={{ lineHeight: '1.6', marginBottom: '10px' }}>
                            - <strong>Primeira jogada:</strong> Rolas todos os 5 dados<br/>
                            - <strong>Segunda jogada:</strong> Escolhes quais dados guardar e rerolas os outros<br/>
                            - <strong>Terceira jogada:</strong> Última oportunidade para melhorar a tua mão<br/>
                            - <strong>Máximo 3 jogadas por turno!</strong>
                        </p>
                    </div>

                    <div style={{ marginBottom: '25px' }}>
                        <h3 style={{ color: '#2196F3', marginBottom: '15px', fontSize: '1.3em' }}>5. Fim da Ronda</h3>
                        <p style={{ lineHeight: '1.6', marginBottom: '10px' }}>
                            - Todos os jogadores completam o seu turno<br/>
                            - As mãos são comparadas automaticamente<br/>
                            - O vencedor leva todo o pote de apostas<br/>
                            - Nova ronda começa com o próximo jogador
                        </p>
                    </div>
                </div>

                <div style={{
                    backgroundColor: '#2d2d2d',
                    padding: '30px',
                    borderRadius: '10px',
                    marginBottom: '30px'
                }}>
                    <h2 style={{ color: '#007bff', marginBottom: '30px', fontSize: '1.8em' }}>
                        Ranking das Mãos (Do Melhor para o Pior)
                    </h2>

                    <div style={{ display: 'grid', gap: '15px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', padding: '15px', backgroundColor: '#3d3d3d', borderRadius: '5px' }}>
                            <span style={{ fontSize: '1.5em', marginRight: '15px' }}></span>
                            <div>
                                <strong style={{ color: '#4CAF50' }}>Five of a Kind</strong> - 5 dados com o mesmo valor
                            </div>
                        </div>

                        <div style={{ display: 'flex', alignItems: 'center', padding: '15px', backgroundColor: '#3d3d3d', borderRadius: '5px' }}>
                            <span style={{ fontSize: '1.5em', marginRight: '15px' }}></span>
                            <div>
                                <strong style={{ color: '#FF9800' }}>Four of a Kind</strong> - 4 dados com o mesmo valor
                            </div>
                        </div>

                        <div style={{ display: 'flex', alignItems: 'center', padding: '15px', backgroundColor: '#3d3d3d', borderRadius: '5px' }}>
                            <span style={{ fontSize: '1.5em', marginRight: '15px' }}></span>
                            <div>
                                <strong style={{ color: '#9C27B0' }}>Full House</strong> - 3 dados iguais + 2 dados iguais
                            </div>
                        </div>

                        <div style={{ display: 'flex', alignItems: 'center', padding: '15px', backgroundColor: '#3d3d3d', borderRadius: '5px' }}>
                            <span style={{ fontSize: '1.5em', marginRight: '15px' }}></span>
                            <div>
                                <strong style={{ color: '#E91E63' }}>Straight</strong> - Sequência de 5 valores
                            </div>
                        </div>

                        <div style={{ display: 'flex', alignItems: 'center', padding: '15px', backgroundColor: '#3d3d3d', borderRadius: '5px' }}>
                            <span style={{ fontSize: '1.5em', marginRight: '15px' }}></span>
                            <div>
                                <strong style={{ color: '#2196F3' }}>Three of a Kind</strong> - 3 dados com o mesmo valor
                            </div>
                        </div>

                        <div style={{ display: 'flex', alignItems: 'center', padding: '15px', backgroundColor: '#3d3d3d', borderRadius: '5px' }}>
                            <span style={{ fontSize: '1.5em', marginRight: '15px' }}></span>
                            <div>
                                <strong style={{ color: '#FFC107' }}>Two Pairs</strong> - 2 pares de dados
                            </div>
                        </div>

                        <div style={{ display: 'flex', alignItems: 'center', padding: '15px', backgroundColor: '#3d3d3d', borderRadius: '5px' }}>
                            <span style={{ fontSize: '1.5em', marginRight: '15px' }}></span>
                            <div>
                                <strong style={{ color: '#8BC34A' }}>One Pair</strong> - 1 par de dados
                            </div>
                        </div>

                        <div style={{ display: 'flex', alignItems: 'center', padding: '15px', backgroundColor: '#3d3d3d', borderRadius: '5px' }}>
                            <span style={{ fontSize: '1.5em', marginRight: '15px' }}></span>
                            <div>
                                <strong style={{ color: '#FF5722' }}>Bust</strong> - Nenhuma combinação (mais alto conta)
                            </div>
                        </div>
                    </div>
                </div>

                <div style={{
                    backgroundColor: '#2d2d2d',
                    padding: '30px',
                    borderRadius: '10px',
                    marginBottom: '30px'
                }}>
                    <h2 style={{ color: '#007bff', marginBottom: '20px', fontSize: '1.8em' }}>
                        Desenvolvido por
                    </h2>
                    <div style={{ textAlign: 'center' }}>
                        <p style={{ fontSize: '1.2em', marginBottom: '10px' }}>
                            <strong>Luka Roca, Tiago Estrela e Daniel Pereira</strong>
                        </p>
                        <p style={{ color: '#ccc' }}>
                            Instituto Superior de Engenharia de Lisboa<br/>
                            Desenvolvimento de Aplicações Web 2025/26
                        </p>
                    </div>
                </div>

                <div style={{ display: 'flex', gap: '5px', justifyContent: 'center', marginTop: '30px' }}>
                    <button
                        onClick={handleCancel}
                        style={{
                            padding: '10px 20px',
                            backgroundColor: '#6c757d',
                            color: 'white',
                            border: 'none',
                            borderRadius: '5px',
                            cursor: 'pointer'
                        }}
                    >
                        Voltar atrás
                    </button>
                </div>
            </div>
        </div>
    );
};