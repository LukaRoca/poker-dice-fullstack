import React, { useState } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuthentication } from '../providers/AuthenticationProvider';
import { NavBarBase, AuthenticationNavBar } from '../components/layout/NavBar';
import '../styles/LoginPage.css';
import {authService, type InviteResponse} from "../services/api/authServices.ts";

export const InvitationsPage: React.FC = () => {
    const [user] = useAuthentication();
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [newInvite, setNewInvite] = useState<InviteResponse | null>(null);

    if (!user) {
        return <Navigate to="/login" replace />;
    }

    const handleCreateInvite = async () => {
        setIsLoading(true);
        setError(null);
        setNewInvite(null);

        try {
            const result = await authService.createInvite();

            if (result.success) {
                setNewInvite(result.value);
            } else {
                setError(result.error || 'Não foi possível gerar o convite.');
            }
        } catch (err) {
            console.error("Erro inesperado:", err);
            setError('Ocorreu um erro ao tentar gerar o código.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex flex-col" style={{ backgroundColor: '#242424' }}>
            <NavBarBase>
                <AuthenticationNavBar name={user} />
            </NavBarBase>

            <div className="login-container" style={{ marginTop: '100px' }}>
                <h1 className="login-header">Criar Convite</h1>

                <p style={{ textAlign: 'center', marginBottom: '30px', color: '#007bff' }}>
                    Utilize este código para convidar os teus amigos
                </p>

                {newInvite && (
                    <div className="invite-result-box" style={{
                        backgroundColor: '#2d2d2d',
                        padding: '20px',
                        borderRadius: '8px',
                        border: '1px solid #4caf50',
                        marginBottom: '20px',
                        textAlign: 'center'
                    }}>
                        <p style={{ color: '#007bff', fontWeight: 'bold' }}>Código gerado com sucesso!</p>
                        <div style={{
                            fontSize: '2em',
                            letterSpacing: '1px',
                            margin: '20px 0',
                            color: 'white',
                            fontFamily: 'monospace'
                        }}>
                            {newInvite.inviteCode}
                        </div>
                        <span style={{ fontSize: '1em', color: '#888' }}>
                            Expira em: {newInvite.expiresAt}
                        </span>
                    </div>
                )}

                {error && (
                    <div className="error-message" style={{
                        backgroundColor: '#ff444420',
                        color: '#ff4444',
                        padding: '10px',
                        borderRadius: '5px',
                        textAlign: 'center',
                        marginBottom: '20px'
                    }}>
                        {error}
                    </div>
                )}

                <button
                    onClick={handleCreateInvite}
                    disabled={isLoading}
                    className="login-button"
                    style={{
                        marginTop: '10px',
                        cursor: isLoading ? 'not-allowed' : 'pointer',
                        opacity: isLoading ? 0.7 : 1
                    }}
                >
                    {isLoading ? 'A gerar...' : 'Gerar código de convite'}
                </button>
            </div>
        </div>
    );
};