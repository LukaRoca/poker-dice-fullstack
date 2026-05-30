import React, { useReducer, useEffect } from 'react';
import { useNavigate, Link, Navigate } from 'react-router-dom';
import { useAuthentication } from "../../providers/AuthenticationProvider.tsx";
import { lobbyService, type CreateLobbyCredentials } from "../../services/api/lobbies.ts";
import '../../styles/lobbyCreation.css';
import {isOk} from "../../services/api/utils.ts";
import {AuthenticationNavBar, NavBarBase} from "../layout/NavBar.tsx";

type State =
    | {
    type: 'editing';
    inputs: CreateLobbyCredentials;
    error: string | null;
}
    | {
    type: 'submitting';
    inputs: CreateLobbyCredentials;
}
    | {
    type: 'success';
    lobbyId: number;
};

type Action =
    | { type: 'edit'; field: string; value: string | number | boolean }
    | { type: 'submit' }
    | { type: 'submitSuccess'; lobbyId: number }
    | { type: 'setError'; error: string };

const initialState: Extract<State, { type: 'editing' }> = {
    type: 'editing',
    inputs: {
        name: '',
        isPublic: true,
        rounds: 3,
        expectedPlayers: 2,
        ante: 1,
        timeout: 10
    },
    error: null,
};

function reduce(state: State, action: Action): State {
    switch (state.type) {
        case 'editing':
            switch (action.type) {
                case 'edit':
                    return {
                        ...state,
                        inputs: {
                            ...state.inputs,
                            [action.field]: action.value,
                        },
                        error: null,
                    };
                case 'submit':
                    return {
                        type: 'submitting',
                        inputs: state.inputs,
                    };
                case 'setError':
                    return {
                        ...state,
                        error: action.error,
                    };
                default:
                    return state;
            }

        case 'submitting':
            switch (action.type) {
                case 'submitSuccess':
                    return {
                        type: 'success',
                        lobbyId: action.lobbyId,
                    };
                case 'setError':
                    return {
                        type: 'editing',
                        inputs: state.inputs,
                        error: action.error,
                    };
                default:
                    return state;
            }

        case 'success':
            return state;
    }
}

class Player {
    id: number;
    name: string;

    constructor(data: any) {
        this.id = data.id;
        this.name = data.name;
    }
}
export class Lobby {
    id: number;
    name: string;
    description: string;
    host: Player;
    rounds: number;
    ante: number;
    expectedPlayers: number;
    state: string;
    players: Player[];

    constructor(data: any) {
        this.id = data.id;
        this.name = data.name;
        this.description = data.description;
        this.host = new Player(data.host);
        this.rounds = data.rounds;
        this.ante = data.ante;
        this.expectedPlayers = data.expectedPlayers;
        this.state = data.state;
        this.players = data.players.map((p: any) => new Player(p));
    }
}
export const LobbyCreationPage: React.FC = () => {
    useNavigate();
    const [user] = useAuthentication();
    const [state, dispatch] = useReducer(reduce, initialState);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        const { name, value, type } = e.target;

        let parsedValue: string | number | boolean = value;

        if (type === 'number') {
            parsedValue = parseInt(value, 10);
        }
        if (type === 'checkbox') {
            parsedValue = (e.target as HTMLInputElement).checked;
        }

        dispatch({ type: 'edit', field: name, value: parsedValue });
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (state.type !== 'editing') return;
        dispatch({ type: 'submit' });
    };

    useEffect(() => {
        if (state.type === 'submitting') {
            const runSubmit = async () => {
                const payload = {
                    ...state.inputs,
                    timeout: state.inputs.timeout * 1000
                };

                const result = await lobbyService.createLobby(payload);

                console.log('🔍 Create Lobby Response:', result);

                if (isOk(result)) {
                    console.log('✅ Lobby created with ID:', result.value.id);
                    dispatch({ type: 'submitSuccess', lobbyId: result.value.id });
                } else {
                    const message = result.error || 'Erro ao criar lobby.';
                    dispatch({ type: 'setError', error: message });
                }
            };
            runSubmit();
        }
    }, [state.type, state.type === 'submitting' ? state.inputs : null]);

    if (!user) {
        return <Navigate to="/login" replace />;
    }

    if (state.type === 'success') {
        return <Navigate to={`/lobby/${state.lobbyId}`} replace />;
    }

    const isLoading = state.type === 'submitting';
    const inputs = state.type === 'editing' || state.type === 'submitting'
        ? state.inputs
        : initialState.inputs;

    return (
        <div className="lobby-creation-container">
            <NavBarBase>
                <AuthenticationNavBar name={user} />
            </NavBarBase>
            <div className="lobby-creation-wrapper">
                <h1 className="lobby-creation-header">Criar Novo Lobby</h1>

                <form onSubmit={handleSubmit} className="lobby-creation-form">
                    <fieldset disabled={isLoading}>
                        <div className="form-group">
                            <label htmlFor="name">Nome do Lobby</label>
                            <input
                                id="name"
                                name="name"
                                type="text"
                                value={inputs.name}
                                onChange={handleChange}
                                required
                                className="form-input"
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="isPublic">Lobby Público?</label>
                            <input
                                id="isPublic"
                                name="isPublic"
                                type="checkbox"
                                checked={inputs.isPublic}
                                onChange={handleChange}
                                className="form-checkbox"
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="rounds">Número de Rondas</label>
                            <input
                                id="rounds"
                                name="rounds"
                                type="number"
                                value={inputs.rounds}
                                onChange={handleChange}
                                required
                                min="1"
                                max="10"
                                className="form-input"
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="expectedPlayers">Máximo de Jogadores</label>
                            <input
                                id="expectedPlayers"
                                name="expectedPlayers"
                                type="number"
                                value={inputs.expectedPlayers}
                                onChange={handleChange}
                                required
                                min="2"
                                max="8"
                                className="form-input"
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="ante">Aposta Inicial (Ante)</label>
                            <input
                                id="ante"
                                name="ante"
                                type="number"
                                value={inputs.ante}
                                onChange={handleChange}
                                required
                                min="1"
                                className="form-input"
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="timeout">Timeout de vida do lobby (segundos)</label>
                            <input
                                id="timeout"
                                name="timeout"
                                type="number"
                                value={inputs.timeout}
                                onChange={handleChange}
                                required
                                min="10"
                                className="form-input"
                            />
                        </div>

                        {state.type === 'editing' && state.error && (
                            <p className="error-message">{state.error}</p>
                        )}

                        <button type="submit" disabled={isLoading} className="form-button">
                            {isLoading ? 'A criar...' : 'Criar Lobby'}
                        </button>
                    </fieldset>
                </form>

                <p className="form-footer">
                    <Link to="/lobbies" style={{ marginLeft: '5px' }}>
                        Cancelar e voltar
                    </Link>
                </p>
            </div>
        </div>
    );
};