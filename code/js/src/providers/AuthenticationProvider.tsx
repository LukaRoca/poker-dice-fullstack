import React, { createContext, useContext, useState, useEffect } from 'react';
import { useSSEEmitter } from './SSEContext';

interface AuthContextType {
    name: string | undefined;
    setName: (newName: string) => void;
    clearName: () => void;
}

const AuthenticationContext = createContext<AuthContextType>({
    name: undefined,
    setName: () => {},
    clearName: () => {},
});


//faz com que nunca temos uma conexão SSE aberta se não houver um utilizador autenticado
export function AuthenticationProvider({ children }: { children: React.ReactNode }) {
    const [name, setName] = useState<string | undefined>(() =>
        localStorage.getItem('name') || undefined
    );

    const { connectSSE, disconnectSSE} = useSSEEmitter();

    const clearName = () => {
        setName(undefined); //limpa react
        localStorage.removeItem('name'); //apaga do browser
    };

    useEffect(() => {
        if (name) { //o nome passa a algo (era undifined) por causa do loggin, ve que exise um nome e chama connectSSE
            console.log('🔐 User authenticated, connecting SSE...');
            connectSSE().catch(error => {
                console.warn('⚠️ SSE connection failed:', error);
            });
        } else {
            console.log('🚪 User logged out, disconnecting SSE...');
            disconnectSSE();
        }
    }, [name, connectSSE, disconnectSSE]);

    const value: AuthContextType = {
        name: name,
        setName: (newName: string) => {
            localStorage.setItem('name', newName); //guarda no browser
            setName(newName); //atualiza react
        },
        clearName,
    };

    return (
        <AuthenticationContext.Provider value={value}>
            {children}
        </AuthenticationContext.Provider>
    );
}

export function useAuthentication() {
    const state = useContext(AuthenticationContext);

    return [
        state.name,
        (name: string) => {
            state.setName(name);
        },
        state.clearName,
    ] as const;
}