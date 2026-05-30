import React, { createContext, useCallback, useContext, useRef, useState } from 'react';

interface BaseEvent {
    type: string;
    [key: string]: any;
}

type EventHandler = (data: any) => void;

interface SSEEmitterContextType {
    connectSSE: () => Promise<void>;
    disconnectSSE: () => void;
    isSSEConnected: boolean;
    registerHandler: (eventType: string, handler: EventHandler) => void;
    unregisterHandler: (eventType: string) => void;
}

const SSEEmitterContext = createContext<SSEEmitterContextType | undefined>(undefined);

export function SSEEmitterProvider({ children }: { children: React.ReactNode }) {
    const emitterRef = useRef<EventSource | null>(null);
    const [isSSEConnected, setIsConnected] = useState(false);

    const handlers = useRef(new Map<string, EventHandler>());

    const handleMessage = (event: MessageEvent) => {
        try {
            const data: BaseEvent = JSON.parse(event.data);
            const handler = handlers.current.get(data.type);
            if (handler) {
                handler(data);
            }
        } catch (e) {
            console.error('Raw SSE data error:', event.data, e);
        }
    };

    const registerHandler = useCallback((eventType: string, handler: EventHandler) => {
        handlers.current.set(eventType, handler);
    }, []);

    const unregisterHandler = useCallback((eventType: string) => {
        handlers.current.delete(eventType);
    }, []);

    const connectSSE = useCallback(() => {
        return new Promise<void>((resolve) => {
            if (!emitterRef.current) {
                const token = localStorage.getItem('authToken') || localStorage.getItem('token');

                if (token) {
                    console.log("🍪 A criar cookie de autenticação...");
                    document.cookie = `token=${token}; path=/; SameSite=Lax`;
                } else {
                    console.warn("⚠️ AVISO: Nenhum token encontrado no localStorage!");
                }

                const sseUrl = '/api/users/listen';

                emitterRef.current = new EventSource(sseUrl, {
                    withCredentials: true
                });

                emitterRef.current.onopen = () => {
                    console.log("✅ SSE Conectado com sucesso!");
                    setIsConnected(true);
                    resolve();
                };

                emitterRef.current.onerror = (err) => {
                    console.error("❌ Erro na conexão SSE. Verifica se o Cookie 'token' existe na aba Application.", err);
                    setIsConnected(false);
                    if (emitterRef.current) {
                        emitterRef.current.close();
                        emitterRef.current = null;
                    }
                    resolve();
                };

                emitterRef.current.addEventListener('message', handleMessage);
            } else {
                resolve();
            }
        });
    }, []);

    const disconnectSSE = useCallback(() => {
        if (emitterRef.current) {
            emitterRef.current.removeEventListener('message', handleMessage);
            emitterRef.current.close();
            emitterRef.current = null;
            setIsConnected(false);
            handlers.current.clear();
            document.cookie = "token=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;"; //para eliminar o cookie imediatamente visto ser no passado
        }
    }, []);

    return (
        <SSEEmitterContext.Provider value={{
            connectSSE,
            disconnectSSE,
            isSSEConnected,
            registerHandler,
            unregisterHandler
        }}>
            {children}
        </SSEEmitterContext.Provider>
    );
}

export function useSSEEmitter() {
    const context = useContext(SSEEmitterContext);
    if (context === undefined) {
        throw new Error('useSSEEmitter must be used within a SSEEmitterProvider');
    }
    return context;
}