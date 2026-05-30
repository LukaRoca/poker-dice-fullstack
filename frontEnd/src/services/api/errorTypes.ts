export const ErrorMessages = {
    USER: {
        INVALID_CREDENTIALS: 'Credenciais inválidas',
        ALREADY_EXISTS: 'Utilizador já existe',
        INSECURE_PASSWORD: 'Password não cumpre os requisitos de segurança',
        NOT_FOUND: 'Utilizador não encontrado',
        INVITATION_INVALID: 'Código de convite inválido',
        INVITATION_EXPIRED: 'Código de convite expirado',
        INVITATION_USED: 'Código de convite já utilizado',
        INVITATION_REQUIRED: 'Código de convite é obrigatório'
    },

    LOBBY: {
        ALREADY_EXISTS: 'Já existe um lobby com este nome',
        NOT_FOUND: 'Lobby não encontrado',
        FULL: 'Lobby está cheio',
        ALREADY_IN_LOBBY: 'Já se encontra neste lobby',
        NOT_IN_LOBBY: 'Não está neste lobby',
        NOT_HOST: 'Apenas o host do lobby pode realizar esta ação',
        CLOSED: 'Lobby foi fechado',
        NONE_AVAILABLE: 'Não existem lobbies disponíveis'
    },

    MATCH: {
        NOT_ENOUGH_PLAYERS: 'São necessários pelo menos 2 jogadores',
        ALREADY_STARTED: 'A partida já começou',
        NOT_FOUND: 'Partida não encontrada'
    },

    GENERAL: {
        UNAUTHORIZED: 'Não tem permissão para realizar esta ação',
        FORBIDDEN: 'Acesso negado',
        NOT_FOUND: 'Recurso não encontrado',
        INVALID_REQUEST: 'Pedido inválido',
        NETWORK_ERROR: 'Erro de conexão',
        UNKNOWN_ERROR: 'Erro desconhecido'
    }
} as const;

export const ErrorTypeMap: Record<string, string> = {
    'user-or-password-invalid': ErrorMessages.USER.INVALID_CREDENTIALS,
    'user-already-exists': ErrorMessages.USER.ALREADY_EXISTS,
    'insecure-password': ErrorMessages.USER.INSECURE_PASSWORD,
    'invitation-dont-exist': ErrorMessages.USER.INVITATION_INVALID,
    'invitation-expired': ErrorMessages.USER.INVITATION_EXPIRED,
    'invitation-used': ErrorMessages.USER.INVITATION_USED,
    'invitation-required': ErrorMessages.USER.INVITATION_REQUIRED,
    'lobby-already-exists': ErrorMessages.LOBBY.ALREADY_EXISTS,
    'lobby-not-found': ErrorMessages.LOBBY.NOT_FOUND,
    'lobby-full': ErrorMessages.LOBBY.FULL,
    'already-in-lobby': ErrorMessages.LOBBY.ALREADY_IN_LOBBY,
    'not-in-lobby': ErrorMessages.LOBBY.NOT_IN_LOBBY,
}
