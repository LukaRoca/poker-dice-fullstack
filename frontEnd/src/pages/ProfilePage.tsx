import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthentication } from "../providers/AuthenticationProvider.tsx";
import { AuthenticationNavBar, NavBarBase } from "../components/layout/NavBar.tsx";
import { authService, type User } from "../services/api/authServices.ts";

export function ProfilePage() {
    const [currentUser] = useAuthentication();
    const navigate = useNavigate();

    // Estado para guardar os dados completos do utilizador vindos da API
    const [userProfile, setUserProfile] = useState<User | null>(null);
    const [isLoadingProfile, setIsLoadingProfile] = useState(true);

    // Estado para o modal de depósito
    const [isDepositModalOpen, setIsDepositModalOpen] = useState(false);
    const [depositAmount, setDepositAmount] = useState<number | string>('');
    const [error, setError] = useState<string | null>(null);
    const [isDepositing, setIsDepositing] = useState(false);

    // Redirecionar se não estiver logado (validação básica de frontend)
    useEffect(() => {
        if (!currentUser) {
            // Opcional: navegar para login se quiseres forçar
            // navigate('/login');
        }
    }, [currentUser, navigate]);

    // Buscar dados do perfil (Stats + Saldo) ao montar o componente
    useEffect(() => {
        let isMounted = true;

        async function loadProfile() {
            const result = await authService.getMe();
            if (isMounted) {
                if (result.success) {
                    setUserProfile(result.value);
                    // Atualizar localStorage para manter consistência caso seja usado noutros lados
                    localStorage.setItem('userBalance', result.value.balance.toString());
                } else {
                    console.error("Falha ao carregar perfil:", result.error);
                }
                setIsLoadingProfile(false);
            }
        }

        loadProfile();

        return () => { isMounted = false; };
    }, []);

    const handleCancel = () => navigate(-1);

    const handleOpenDeposit = () => {
        setError(null);
        setDepositAmount('');
        setIsDepositModalOpen(true);
    }

    const handleDepositSubmit = async () => {
        const amount = Number(depositAmount);

        if (!amount || amount <= 0) {
            setError("Insira um valor válido.");
            return;
        }

        setIsDepositing(true);
        setError(null);

        const result = await authService.deposit(amount);

        setIsDepositing(false);

        if (result.success) {
            const newBalance = result.value.newBalance;

            // Atualizar o estado local do perfil com o novo saldo
            if (userProfile) {
                setUserProfile({ ...userProfile, balance: newBalance });
            }

            // Atualizar localStorage
            localStorage.setItem('userBalance', newBalance.toString());

            setIsDepositModalOpen(false);
        } else {
            setError(result.error || "Erro ao efetuar depósito.");
        }
    };

    // Renderização condicional enquanto carrega
    if (!currentUser) return <div>A redirecionar para login...</div>;
    if (isLoadingProfile) return (
        <div className="min-h-screen flex items-center justify-center" style={{ backgroundColor: '#242424', color: 'white' }}>
            <p>A carregar perfil...</p>
        </div>
    );

    return (
        <div className="min-h-screen flex flex-col" style={{ backgroundColor: '#242424' }}>
            <NavBarBase>
                <AuthenticationNavBar name={currentUser} />
            </NavBarBase>

            <div style={{
                flexGrow: 1,
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                padding: '20px',
                marginTop: '80px'
            }}>
                <div style={{
                    maxWidth: '600px',
                    width: '100%',
                    backgroundColor: '#2d2d2d',
                    padding: '30px',
                    borderRadius: '10px',
                    color: 'white',
                    position: 'relative'
                }}>
                    <h1 style={{ fontSize: '2.5em', marginBottom: '20px', textAlign: 'center', marginTop: '10px' }}>
                        Perfil do Utilizador
                    </h1>

                    <div style={{ marginBottom: '15px' }}>
                        <h2 style={{ color: '#007bff', marginBottom: '10px', fontSize: '1.5em' }}>
                            Informações Pessoais
                        </h2>
                        <div style={{ display: 'grid', gap: '5px' }}>
                            <p><strong>Nome:</strong> {userProfile?.name}</p>
                            <p><strong>ID:</strong> {userProfile?.id}</p>
                            <p><strong>Saldo:</strong> {userProfile?.balance} coins</p>
                        </div>
                    </div>

                    <div style={{ marginBottom: '15px' }}>
                        <h2 style={{ color: '#007bff', marginBottom: '10px', fontSize: '1.5em' }}>
                            Estatísticas de Jogo
                        </h2>
                        <div style={{ display: 'grid', gap: '5px' }}>
                            <p><strong>Rondas jogadas:</strong> {userProfile?.roundsPlayed ?? 0}</p>
                            <p><strong>Rondas ganhas:</strong> {userProfile?.roundsWon ?? 0}</p>
                            <p><strong>Taxa de vitórias:</strong> {userProfile?.winRate ? userProfile.winRate.toFixed(1) : '0.0'}%</p>
                            {/* "Melhor mão" ainda não vem da API, mantemos estático ou removemos se não for usado */}
                            {/* <p><strong>Melhor mão:</strong> Nenhuma</p> */}
                        </div>
                    </div>

                    <div style={{ display: 'flex', gap: '15px', justifyContent: 'center', marginTop: '40px' }}>
                        <button
                            onClick={handleOpenDeposit}
                            style={{
                                padding: '10px 20px',
                                backgroundColor: '#007bff',
                                color: 'white',
                                border: 'none',
                                borderRadius: '5px',
                                cursor: 'pointer',
                                fontSize: '1em'
                            }}>
                            Depositar
                        </button>

                        <button
                            onClick={handleCancel}
                            style={{
                                padding: '10px 20px',
                                backgroundColor: '#6c757d',
                                color: 'white',
                                border: 'none',
                                borderRadius: '5px',
                                cursor: 'pointer',
                                fontSize: '1em'
                            }}>
                            Voltar atrás
                        </button>
                    </div>

                    {isDepositModalOpen && (
                        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.7)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
                            <div style={{ backgroundColor: '#333', padding: '25px', borderRadius: '10px', width: '300px', textAlign: 'center', border: '1px solid #555' }}>
                                <h3 style={{ marginBottom: '20px', color: 'white' }}>Depositar Créditos</h3>

                                <input
                                    type="number"
                                    value={depositAmount}
                                    onChange={(e) => setDepositAmount(e.target.value)}
                                    placeholder="0"
                                    min="1"
                                    style={{
                                        width: '100%',
                                        padding: '10px',
                                        marginBottom: '20px',
                                        borderRadius: '5px',
                                        border: '1px solid #555',
                                        backgroundColor: '#444',
                                        color: 'white',
                                        fontSize: '1.2em',
                                        textAlign: 'center'
                                    }}
                                />

                                {error && <p style={{ color: '#ff6b6b', fontSize: '0.9em', marginBottom: '15px' }}>{error}</p>}

                                <div style={{ display: 'flex', gap: '10px', justifyContent: 'center' }}>
                                    <button onClick={handleDepositSubmit} disabled={isDepositing} style={{ padding: '8px 16px', backgroundColor: isDepositing ? '#113d9f' : '#007bff', color: 'white', border: 'none', borderRadius: '5px', cursor: isDepositing ? 'not-allowed' : 'pointer', width: '100px' }}>
                                        {isDepositing ? '...' : 'OK'}
                                    </button>
                                    <button onClick={() => setIsDepositModalOpen(false)} style={{ padding: '8px 16px', backgroundColor: '#dc3545', color: 'white', border: 'none', borderRadius: '5px', cursor: 'pointer', width: '100px' }}>
                                        Cancelar
                                    </button>
                                </div>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}