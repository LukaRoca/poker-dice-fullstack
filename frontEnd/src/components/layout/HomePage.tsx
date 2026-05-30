
import { Link } from "react-router-dom";
import { useAuthentication } from "../../providers/AuthenticationProvider.tsx";
import '../../styles/App.css';
import {AuthenticationNavBar, NavBarBase, NoAuthenticationNavBar} from "./NavBar.tsx";

export function HomePage() {
    const [user] = useAuthentication();

    return (
        <div className="min-h-screen flex flex-col" style={{ backgroundColor: '#242424' }}>
            <NavBarBase>
                {user ? <AuthenticationNavBar name={user} /> : <NoAuthenticationNavBar />}
            </NavBarBase>

            <div style={{
                flexGrow: 1,
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                marginTop: '60px',
                width: '100%'
            }}>
                <div style={{
                    maxWidth: '800px',
                    textAlign: 'center',
                    padding: '0 20px'
                }}>
                    <h1 style={{ fontSize: '4em', marginBottom: '20px', color: 'white', lineHeight: '1.1' }}>
                        Bem-vindo ao <br />
                        <span style={{ color: '#007bff' }}>PokerDice</span>
                    </h1>
                    <p style={{ fontSize: '1.3em', color: '#ccc', marginBottom: '40px', lineHeight: '1.6' }}>
                        Um jogo de estratégia, dados e apostas.
                        <br />
                        Junta-te a uma sala e desafia os teus amigos!
                    </p>

                    {!user && (
                        <Link
                            to="/register"
                            className="nav-btn btn-register"
                            style={{ fontSize: '1.1em', padding: '15px 40px' }}
                        >
                            Começar a Jogar Agora
                        </Link>
                    )}

                    {user && (
                        <Link
                            to="/lobbies"
                            className="nav-btn btn-register"
                            style={{ fontSize: '1.1em', padding: '15px 40px', backgroundColor: '#007bff' }}
                        >
                            Jogar Agora
                        </Link>
                    )}
                </div>
            </div>
        </div>
    );
}