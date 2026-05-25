import {ErrorMessages, ErrorTypeMap} from "./errorTypes.ts";

export type Result<T> = //para n termos de usar tantos try catch
    | { success: true; value: T }
    | { success: false; error: string };

export const isOk = <T>(result: Result<T>): result is { success: true; value: T } => result.success;

function extractErrorMessage(err: unknown): string { //se o servidor cair, erro de internet ou erro de js, converte para um erro de string
    if (err instanceof Error) return err.message;
    if (typeof err === 'string') return err;
    try {
        return JSON.stringify(err);
    } catch {
        return 'Unknown error';
    }
}

function getErrorMessage(errorData: any): string { //erros de servidor (palavras passe errada, user n existe, etc)
    if (!errorData?.type) {
        return errorData?.title || errorData?.detail || errorData?.message || ErrorMessages.GENERAL.UNKNOWN_ERROR;
    }

    const errorType = errorData.type.split('/').pop(); //tira a barra e fica so com o tipo do erro presente no errorTypes
    return ErrorTypeMap[errorType] || errorData?.title || errorData.type;
}

export async function fetchWrapper<T>(
    url: string,
    options: RequestInit = {}
): Promise<Result<T>> {
    try {
        const token = localStorage.getItem('authToken');

        const headers: Record<string, string> = {
            'Content-Type': 'application/json',
            'Cache-Control': 'no-cache, no-store, must-revalidate',
            'Pragma': 'no-cache',
            'Expires': '0',
        };

        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        let finalUrl = url;
        if (!options.method || options.method === 'GET') {
            const separator = url.includes('?') ? '&' : '?';
            finalUrl = `${url}${separator}_=${Date.now()}`; //para o impedir o browser de mostrar dados velhos
        }

        const response = await fetch(finalUrl, {
            headers: {
                ...headers,
                ...options.headers,
            },
            credentials: 'include', //para mandar cookies
            ...options,
        });

        if (!response.ok) {
            let errorData: any = null;
            try {
                errorData = await response.json();
            } catch {}
            const msg = getErrorMessage(errorData) || `Request failed with status ${response.status}`;
            return { success: false, error: msg };
        }

        if (response.status === 204) {
            return { success: true, value: undefined as T };
        }

        const data = await response.json();
        return { success: true, value: data as T };
    } catch (error) {
        return { success: false, error: extractErrorMessage(error) };
    }
}