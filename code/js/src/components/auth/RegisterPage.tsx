import React, { useReducer, useEffect } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import {useAuthentication} from "../../providers/AuthenticationProvider.tsx";
import {authService} from "../../services/api/authServices.ts";
import {NavBarBase, NoAuthenticationNavBar} from "../layout/NavBar.tsx";
//definimos tipos daquilo que neste caso queremos coo parametros do registo e, dquilo que queremos como condiçoes de uma password valida.
// temos 3 estados, o de edit que é o inicial, o de submit e o success. algumas açoes, e definimos um initialState como edit
// com todos os espaços vazios e os criterios da password a false visto ainda n termos uma pass valida. novamente implementamos uma
// funcion reduce que vai tartar de todo o fluxo dos estados. Criamos mais um modelo react, sendo este a pagina e registo, onde recebe um navigate,
//  um user para poder ser guardado no browser, um invite code e novamente um const state dispatch useReducer que vai tratar de modificar o state tendo em conta o dispatch. Fazemks um useEffect para que se o user ja estiver autenticado, seja automaticamente redirecionado para a pagina obbys, criamos uma funçao para os criterios da password onde recebe tanto a pass como a passconfirm que têm de ser iguais e respeittar todos os criterios da funçao. Criamos um handleCHange para novamente renderizar a pagina automaticamente por meio do react, sendo este usado pelo useReducer que é chamado pelo dispatch sempre que algo for modificado, assim aqui por exemplo enquando o usuario escreve, os parametros da funçao da password vao sendo alterados visto que alguns vao sendo completo e afins. Depois fizemos o handleSubmit que necessita de estar no etado edit porque o user so ao clicar em criar, é que muda para submit, aqui tratamos de possiveis erros como palavras passe nao dao match e, se todos os parametros da computePassowd estao certos, ao fim de verificar tudo, fazemos um dispatch para o tipo submit avisando o useReducer usando react para renderizar a pagina. Apos isto fazemos outro useEffect que consiste em, caso estejamos a submeter este fara o pedido a api para a criaçao do user, por fim verificamos se deu success, se sim, apresentamos um html com sucess, se ainda nao, retornamos no fim um html com a pagina de registo para preeecher
type RegisterInputs = {
    name: string;
    password: string;
    confirmPassword: string;
    inviteCode: string;
};

type PasswordCriteria = {
    minLength: boolean;
    maxLength: boolean;
    hasNumber: boolean;
    hasUppercase: boolean;
    hasLowercase: boolean;
    hasSpecialChar: boolean;
    passwordsMatch: boolean;
};

type State =
    | {
    type: 'editing';
    inputs: RegisterInputs;
    passwordCriteria: PasswordCriteria;
    error: string | null;
}
    | {
    type: 'submitting';
    inputs: RegisterInputs;
    passwordCriteria: PasswordCriteria;
}
    | {
    type: 'success';
};

type Action =
    | { type: 'edit'; field: string; value: string }
    | { type: 'submit' }
    | { type: 'submitSuccess' }
    | { type: 'setError'; error: string }
    | { type: 'updatePasswordCriteria'; criteria: PasswordCriteria }
    | { type: 'setInviteCode'; code: string };

const initialState: Extract<State, { type: 'editing' }> = {
    type: 'editing',
    inputs: {
        name: '',
        password: '',
        confirmPassword: '',
        inviteCode: '',
    },
    passwordCriteria: {
        minLength: false,
        maxLength: true,
        hasNumber: false,
        hasUppercase: false,
        hasLowercase: false,
        hasSpecialChar: false,
        passwordsMatch: false,
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

                case 'updatePasswordCriteria':
                    return {
                        ...state,
                        passwordCriteria: action.criteria,
                    };

                case 'setInviteCode':
                    return {
                        ...state,
                        inputs: { ...state.inputs, inviteCode: action.code },
                    };

                case 'setError':
                    return {
                        ...state,
                        inputs: {
                            ...state.inputs,
                            password: '',
                            confirmPassword: '',
                        },
                        passwordCriteria: initialState.passwordCriteria,
                        error: action.error,
                    };

                case 'submit':
                    return {
                        type: 'submitting',
                        inputs: state.inputs,
                        passwordCriteria: state.passwordCriteria,
                    };

                default:
                    return state;
            }

        case 'submitting':
            switch (action.type) {
                case 'submitSuccess':
                    return {
                        type: 'success',
                    };

                case 'setError':
                    return {
                        type: 'editing',
                        inputs: {
                            ...state.inputs,
                            password: '',
                            confirmPassword: '',
                        },
                        passwordCriteria: initialState.passwordCriteria,
                        error: action.error,
                    };
                default:
                    return state;
            }

        case 'success':
            return state;
    }
}

export const RegisterPage: React.FC = () => {
    const navigate = useNavigate();
    const [user] = useAuthentication();
    const { inviteCode: urlInviteCode } = useParams<{ inviteCode?: string }>();
    const [state, dispatch] = useReducer(reduce, initialState);

    useEffect(() => {
        if (urlInviteCode) {
            dispatch({ type: 'setInviteCode', code: urlInviteCode });
        }
    }, [urlInviteCode]);

    useEffect(() => {
        if (user) {
            navigate('/lobbies', { replace: true });
        }
    }, [user, navigate]);

    function computePasswordCriteria(inputs: RegisterInputs): PasswordCriteria {
        const { password, confirmPassword } = inputs;
        return {
            minLength: password.length >= 8,
            maxLength: password.length <= 256,
            hasNumber: /\d/.test(password),
            hasUppercase: /[A-Z]/.test(password),
            hasLowercase: /[a-z]/.test(password),
            hasSpecialChar: /[^A-Za-z0-9]/.test(password),
            passwordsMatch: password === confirmPassword && password.length > 0,
        };
    }

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const field = e.target.name;
        const value = e.target.value;
        dispatch({ type: 'edit', field, value });

        if (state.type === 'editing' && (field === 'password' || field === 'confirmPassword')) {
            const nextInputs = { ...state.inputs, [field]: value };
            const criteria = computePasswordCriteria(nextInputs);
            dispatch({ type: 'updatePasswordCriteria', criteria });
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (state.type !== 'editing') return;

        const { password, confirmPassword } = state.inputs;
        if (password !== confirmPassword) {
            dispatch({ type: 'setError', error: 'As passwords não coincidem.' });
            return;
        }

        const criteria = computePasswordCriteria(state.inputs);
        dispatch({ type: 'updatePasswordCriteria', criteria });

        const allValid = Object.values(criteria).every(Boolean);
        if (!allValid) {
            dispatch({ type: 'setError', error: 'Por favor, cumpra todos os critérios da password.' });
            return;
        }

        dispatch({ type: 'submit' });
    };

    useEffect(() => {
        if (state.type === 'submitting') {
            const { name, password, inviteCode } = state.inputs;

            const runSubmit = async () => {
                try {
                    const result = await authService.register({
                        name: name.trim(),
                        password,
                        inviteCode: inviteCode || null
                    });
                    if (result.success) {
                        dispatch({ type: 'submitSuccess' });
                    } else {
                        throw new Error(result.error)
                    }
                } catch (err: any) {
                    console.error('Erro de Registo:', err);
                    const message =
                        err?.message ||
                        'Erro no registo. Verifique os dados.';
                    dispatch({ type: 'setError', error: message });
                }
            };
            runSubmit();
        }
    }, [state.type, state.type === 'submitting' ? state.inputs : null]);

    if (state.type === 'success') {
        return (
            <div className="login-container" style={{ textAlign: 'center' }}>
                <h1 className="login-header">Bem-vindo! 🎉</h1>

                <div style={{ margin: '30px 0', color: '#2e7d32' }}>
                    <p style={{ fontSize: '1.1em', marginBottom: '10px' }}>
                        Registou-se com sucesso.
                    </p>
                    <p>A sua conta está pronta a usar.</p>
                </div>

                <button
                    onClick={() => navigate('/login', { replace: true })}
                    className="login-button"
                >
                    Ir para o Login
                </button>
            </div>
        );
    }

    const isLoading = state.type === 'submitting';
    const inputs = (state.type === 'editing' || state.type === 'submitting')
        ? state.inputs
        : initialState.inputs;

    const criteria = (state.type === 'editing' || state.type === 'submitting')
        ? state.passwordCriteria
        : initialState.passwordCriteria;



    return (
        <div className="login-container">
            <NavBarBase>
                <NoAuthenticationNavBar />
            </NavBarBase>
            <h1 className="login-header">Registo</h1>

            <form onSubmit={handleSubmit}>
                <fieldset disabled={isLoading}>
                    <div className="form-group">
                        <label htmlFor="name">Nome de Utilizador</label>
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
                        <label htmlFor="password">Palavra-Passe</label>
                        <input
                            id="password"
                            name="password"
                            type="password"
                            value={inputs.password}
                            onChange={handleChange}
                            required
                            className="form-input"
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="confirmPassword">Confirmar Palavra-Passe</label>
                        <input
                            id="confirmPassword"
                            name="confirmPassword"
                            type="password"
                            value={inputs.confirmPassword}
                            onChange={handleChange}
                            required
                            className="form-input"
                        />
                    </div>
                    <ul className="criteria-list" style={{ listStyleType: 'none', paddingLeft: 0, fontSize: '0.9em', color: '#d9534f' }}>

                        {!criteria.minLength && (
                            <li>× A password deve ter pelo menos 8 caracteres.</li>
                        )}

                        {!criteria.hasNumber && (
                            <li>× Falta um número.</li>
                        )}

                        {!criteria.hasUppercase && (
                            <li>× Falta uma letra maiúscula.</li>
                        )}

                        {!criteria.hasLowercase && (
                            <li>× Falta uma letra minúscula.</li>
                        )}

                        {!criteria.hasSpecialChar && (
                            <li>× Falta um símbolo (ex: ! @ # $).</li>
                        )}

                        {!criteria.passwordsMatch && state.inputs.confirmPassword.length > 0 && (
                            <li>× As passwords não coincidem.</li>
                        )}
                    </ul>

                    <div className="form-group">
                        <label htmlFor="inviteCode">Código de Convite </label>
                        <input
                            id="inviteCode"
                            name="inviteCode"
                            type="text"
                            value={inputs.inviteCode}
                            onChange={handleChange}
                            className="form-input"
                        />
                    </div>

                    {state.type === 'editing' && state.error && (
                        <p className="error-message">{state.error}</p>
                    )}

                    <button type="submit" disabled={!isLoading} className="login-button">
                        {isLoading ? 'A registar...' : 'Registar Conta'}
                    </button>
                </fieldset>
            </form>

            <p className="login-footer">
                Já tem conta?
                <Link to="/login" style={{ marginLeft: '5px' }}>
                    Inicie sessão aqui
                </Link>
            </p>
        </div>
    );
};