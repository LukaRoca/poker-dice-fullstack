import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthentication } from '../providers/AuthenticationProvider';

export const LogoutPage: React.FC = () => {
    const [, , clearUser] = useAuthentication();
    const navigate = useNavigate();
    const [isLoggingOut, setIsLoggingOut] = useState(false);

    const handleLogout = () => {
        setIsLoggingOut(true);
        clearUser();
        localStorage.removeItem('authToken');
        setTimeout(() => {
            navigate('/login', { replace: true });
        }, 1000);
    };

    const handleCancel = () => {
        navigate(-1);
    };

    if (isLoggingOut) {
        return (
            <div className="logout-container" style={{ textAlign: 'center', marginTop: '50px' }}>
                <h1>A Terminar Sessão...</h1>
                <p>Aguarde um momento.</p>
            </div>
        );
    }

    return (
        <div className="logout-container" style={{
            maxWidth: '400px',
            margin: '50px auto',
            padding: '20px',
            textAlign: 'center'
        }}>
            <h1>Terminar Sessão</h1>

            <div style={{ margin: '30px 0' }}>
                <p style={{ fontSize: '1.1em', marginBottom: '20px' }}>
                    Tem a certeza que pretende terminar sessão?
                </p>

                <div style={{ display: 'flex', gap: '15px', justifyContent: 'center' }}>
                    <button
                        onClick={handleCancel}
                        style={{
                            padding: '10px 20px',
                            backgroundColor: '#6c757d',
                            color: 'white',
                            border: 'none',
                            borderRadius: '5px',
                            cursor: 'pointer'
                        }}
                    >
                        Cancelar
                    </button>

                    <button
                        onClick={handleLogout}
                        style={{
                            padding: '10px 20px',
                            backgroundColor: '#dc3545',
                            color: 'white',
                            border: 'none',
                            borderRadius: '5px',
                            cursor: 'pointer'
                        }}
                    >
                        Sim, Terminar Sessão
                    </button>
                </div>
            </div>
        </div>
    );
};