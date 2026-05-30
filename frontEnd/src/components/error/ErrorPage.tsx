import { useNavigate } from 'react-router-dom';

export const ErrorPage = () => {
    const navigate = useNavigate();
    return (
        <div className="min-h-screen flex flex-col" style={{ backgroundColor: '#242424' }}>

            <div style={{
                flexGrow: 1,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                padding: '20px',
                marginTop: '80px'
            }}>
                <div style={{
                    maxWidth: '500px',
                    width: '100%',
                    textAlign: 'center',
                    color: 'white'
                }}>
                    <div style={{ marginBottom: '30px' }}>
                        <div style={{
                            fontSize: '80px',
                            marginBottom: '10px',
                            color: '#dc3545'
                        }}>
                            ⚠️
                        </div>
                    </div>

                    <div style={{
                        backgroundColor: '#2d2d2d',
                        padding: '40px',
                        borderRadius: '10px',
                        border: '1px solid #444'
                    }}>
                        <div style={{ marginBottom: '20px' }}>
                            <h1 style={{
                                fontSize: '2em',
                                marginBottom: '15px',
                                color: '#fff'
                            }}>
                                Página Não Encontrada
                            </h1>
                            <p style={{
                                color: '#ccc',
                                fontSize: '1.1em',
                                lineHeight: '1.5'
                            }}>
                                A página que procura não existe ou foi movida.
                            </p>
                        </div>

                        <div style={{ display: 'flex', gap: '15px', justifyContent: 'center' }}>
                            <button
                                onClick={() => navigate(-1)}
                                style={{
                                    padding: '12px 25px',
                                    backgroundColor: '#6c757d',
                                    color: 'white',
                                    border: 'none',
                                    borderRadius: '5px',
                                    cursor: 'pointer',
                                    fontSize: '1em'
                                }}
                            >
                                Voltar
                            </button>

                            <button
                                onClick={() => navigate('/')}
                                style={{
                                    padding: '12px 25px',
                                    backgroundColor: '#007bff',
                                    color: 'white',
                                    border: 'none',
                                    borderRadius: '5px',
                                    cursor: 'pointer',
                                    fontSize: '1em'
                                }}
                            >
                                Página Inicial
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ErrorPage;