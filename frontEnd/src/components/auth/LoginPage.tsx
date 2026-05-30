import {Link, Navigate, useLocation} from 'react-router-dom';
import '../../styles/LoginPage.css';
import React, {useReducer} from "react";
import {useAuthentication} from "../../providers/AuthenticationProvider";
import {useSSEEmitter} from "../../providers/SSEContext";
import {authService} from "../../services/api/authServices";
import {NavBarBase, NoAuthenticationNavBar} from "../layout/NavBar.tsx";
//Começamos por definir 3 estados: um de editing que consiste no primeiro ao abrir a pagina, onde estamos a editar os campos do ficheiro,
// redirect que consiste no estado que redireciona nos para outras paginas e o submiting que consiste no estado que permite haver
// apenas 1 cloque quando o utilizador pretende submeter neste caso os dados de login, para que n seja possivel alterar campos ou
// clicar novamente quando esta ação esta a ser feita. Definimos tbm açoes que sao as possiveis neste ficheiro.
// A nosssa funºao reduce, é a que permite manusear todos estes estados e açoes, começamos no editing, para cada ação possivel,
// returnamos os inputs dependendo de como devem ficar e outros parametros dos prorpios estados
// criamos um modelo react que consiste na pagina login, definimos state e dispach como uma const, usando useReducer, ou seja,
// sempre que usarmos o dispatch, o state sera alterado com os parametros que o useReducer poporciona,
// como ainda n temos um user apenas nos interessa a funçao que define o user no login , e um connectSSE visto que temos de conectar o user quando
// este autenticar-se ao SSE. Se ja tivermos no estado redirect, somos redirecionados para a pagina de lobbys,
// o nosso handleChange é responsavel por fazer um dispatch alterando a action para o tipo edit visto estarmos a editar o ficheiro,
// é responsavel por ao haver uma alteraçao, este ser chamado verificar em que campo foi e, colocar o valor presente no inputValue,
// na variavel que foi alterada, assim este é enviado ao reducer como definimos acima e o ciclo do mesmo atualiza
// o state renderizando a pagina com react. O Handle submit, serve para alterar do estado editing para submit e,
// fazer o pedido a API do login atravez do authService, se der success e o token for bem recebido,
// atravez do setName definimos o user no browser e fazemos um connectSSE para o mesmo ficar connectado e de seguida um
// dispach para o userReduce novamente atualizando o type para setRedirect. os inputs se estivermos no edit ou submitting ainda,
// voltam a ficar vazios. e depois a pagina HTML e renderizada pelo return.
type State =
    | {type: 'editing',
    inputs:{name: string, password: string},
    showPassword: boolean,
    error: string | null,
    shouldRedirect: boolean,
}
    | {type: 'redirect'}
    | {type: 'submitting',
    inputs:{name: string, password: string},
    showPassword: boolean,
    error: string | null,
    isLoading: boolean,
    shouldRedirect: boolean,
}

type Action =
    | { type: 'edit'; inputName: string; inputValue: string }
    | { type: 'submit'; inputs:{name: string, password: string}}
    | { type: 'togglePassword' }
    | { type: 'setError'; error: string | null }
    | { type: 'setLoading'; isLoading: boolean }
    | { type: 'setRedirect'}

function unexpectedAction(action: Action, state: State) {
    console.log(`Unexpected action ${action.type} in state ${state.type}`)
    return state
}

function reduce(state: State, action: Action): State {
    switch (state.type) {
        case 'editing':
            switch(action.type){
                case 'edit':
                    return { ...state, inputs: { ...state.inputs, [action.inputName]: action.inputValue } }
                case 'submit':
                    return {
                        type: 'submitting',
                        inputs: action.inputs,
                        showPassword: state.showPassword,
                        error: null,
                        isLoading: true,
                        shouldRedirect: false
                    }
                case 'togglePassword':
                    return { ...state, showPassword: !state.showPassword }
                default:
                    unexpectedAction(action, state)
                    return state
            }
        case 'submitting':
            switch(action.type){
                case 'setError': //se der erro limpamos a palavra passe
                    return {
                        type: 'editing',
                        inputs: { ...state.inputs, password: '' },
                        showPassword: false,
                        error: action.error,
                        shouldRedirect: false
                    }
                case 'setRedirect':
                    return { type: 'redirect'}
                default:
                    unexpectedAction(action, state)
                    return state
            }
        default:
            unexpectedAction(action, state)
            return state
    }
}
export function LoginPage() {
    const [state, dispatch] = useReducer(reduce, {
        type: 'editing',
        inputs: { name: '', password: '' },
        showPassword: false,
        error: null,
        shouldRedirect: false,
    })
    const [,setName] = useAuthentication()
    const location = useLocation()
    const { connectSSE } = useSSEEmitter()


    if (state.type === 'redirect') {
        return <Navigate to={location.state?.source || '/lobbies'} replace={true} />
    }

    function handleChange(ev: React.ChangeEvent<HTMLInputElement>) {
        dispatch({ type: 'edit', inputName: ev.target.name, inputValue: ev.target.value })
    }

    async function handleSubmit(ev: React.FormEvent<HTMLFormElement>) {
        ev.preventDefault()
        if (state.type === 'editing') {
            dispatch({ type: 'submit', inputs: state.inputs })
            try {
                const result = await authService.login({
                    name: state.inputs.name,
                    password: state.inputs.password
                });

                if (result.success) {
                    if (result.value.token) {
                        if (typeof setName === 'function') {
                            setName(state.inputs.name)
                        }
                        await connectSSE()
                        dispatch({ type: 'setRedirect'})
                    } else {
                        dispatch({ type: 'setError', error: 'Token não recebido' })
                    }
                } else {
                    dispatch({ type: 'setError', error: result.error })
                }
            } catch (err: any) {
                dispatch({ type: 'setError', error: 'Erro de conexão'})
            }
        }
    }

    const inputs = state.type === 'editing' || state.type === 'submitting'
        ? state.inputs
        : { name: '', password: '' }

    const isLoading = state.type === 'submitting';

    return (
        <div className="login-container">
            <NavBarBase>
                <NoAuthenticationNavBar />
            </NavBarBase>
            <h1 className="login-header">Login</h1>
            <form onSubmit={handleSubmit}>
                <fieldset disabled={isLoading}>
                    <div className="form-group">
                        <div>
                            <label htmlFor="name" className="label">
                                Name
                            </label>
                            <input
                                className="form-input"
                                id="name"
                                type="text"
                                name="name"
                                value={inputs.name}
                                onChange={handleChange}
                                placeholder="Enter your name"
                                required
                            />
                        </div>

                        <div>
                            <label htmlFor="password" className="label">
                                Password
                            </label>
                            <div className="password-container">
                                <input
                                    className="form-input"
                                    id="password"
                                    type={(state.type === 'editing' || state.type === 'submitting') && state.showPassword ? "text" : "password"}
                                    name="password"
                                    value={inputs.password}
                                    onChange={handleChange}
                                    placeholder="Enter your password"
                                    required
                                />
                                <button
                                    type="button"
                                    onClick={() => dispatch({ type: 'togglePassword' })}
                                    className="toggle-password"
                                >
                                    {(state.type === 'editing' || state.type === 'submitting') && state.showPassword ? 'Ocultar palavra passe' : 'Ver palavra passe'}
                                </button>
                            </div>
                        </div>

                        <button type="submit" className="login-button">
                            Login
                        </button>
                    </div>
                </fieldset>

                <div className="login-footer">
                    <p className="signup-text">
                        Don't have an account?{' '}«
                        <Link to="/register" className="signup-link">
                            Sign Up
                        </Link>
                    </p>
                </div>

                {state.type === 'editing' && state.error && (
                    <div className="error-message">{state.error}</div>
                )}

                {state.type === 'submitting' && (
                    <div className="loading">Loading...</div>
                )}
            </form>
        </div>
    );
}
