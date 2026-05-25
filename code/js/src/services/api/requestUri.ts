
const BASE_API_URL = '/api';

export const ApiRoutes = {

    user: {
        profile: `${BASE_API_URL}/user/me`,
        register: `${BASE_API_URL}/register`,
        login: `${BASE_API_URL}/login`,
        logout: `${BASE_API_URL}/user/logout`,
        invite: `${BASE_API_URL}/user/invite`,
        tokenRefresh: `${BASE_API_URL}/token/refresh`,
        listen: `${BASE_API_URL}/user/listen`,
        deposit: `${BASE_API_URL}/user/deposit`,
    },

    lobby: {
        getAll: `${BASE_API_URL}/lobbies`,
        create: `${BASE_API_URL}/lobbies`,
    },

    match: {
        play: (matchId: number | string, roundNumber: number | string) => `${BASE_API_URL}/matches/${matchId}/rounds/${roundNumber}/play`,
        endTurn: (matchId: number | string, roundNumber: number | string) => `${BASE_API_URL}/matches/${matchId}/rounds/${roundNumber}/end-turn`,
        getState: (matchId: number | string) => `${BASE_API_URL}/matches/${matchId}/state`,
    }
};