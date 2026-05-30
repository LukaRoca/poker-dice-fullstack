import { fetchWrapper, type Result } from './utils';
import { ApiRoutes } from "./requestUri";

interface LoginCredentials {
    name: string;
    password: string;
}

interface RegisterCredentials {
    name: string;
    password: string;
    inviteCode: string | null;
}

export interface User {
    id: number;
    name: string;
    balance: number;
    roundsPlayed: number;
    roundsWon: number;
    winRate: number;
}

interface LoginResponse {
    token: string;
    user: {
        id: number;
        name: string;
        balance: number;
    };
}

interface DepositResponse {
    newBalance: number;
}

export interface InviteResponse {
    inviteCode: string;
    expiresAt: string;
}

export const authService = {
    login(credentials: LoginCredentials): Promise<Result<LoginResponse>> {
        return fetchWrapper<LoginResponse>(ApiRoutes.user.login, {
            method: 'POST',
            body: JSON.stringify(credentials),
        }).then(result => {
            if (result.success && result.value.token) {
                localStorage.setItem('authToken', result.value.token);
                localStorage.setItem('userId', result.value.user.id.toString());
                localStorage.setItem('userBalance', result.value.user.balance.toString());
            }
            return result;
        });
    },

    register(credentials: RegisterCredentials): Promise<Result<any>> {
        return fetchWrapper(ApiRoutes.user.register, {
            method: 'POST',
            body: JSON.stringify(credentials),
        });
    },

    deposit(amount: number): Promise<Result<DepositResponse>> {
        return fetchWrapper<DepositResponse>(ApiRoutes.user.deposit, {
            method: 'POST',
            body: JSON.stringify({ amount: amount }),
        });
    },

    createInvite(): Promise<Result<InviteResponse>> {
        const url = ApiRoutes.user.invite || '/api/users/invite';
        return fetchWrapper<InviteResponse>(url, {
            method: 'POST'
        });
    },

    getMe(): Promise<Result<User>> {

        return fetchWrapper<User>(ApiRoutes.user.profile);
    }
};