import {Link} from "react-router-dom";
import React from "react";

export function NoAuthenticationNavBar() {
    return (
        <div className="nav-links">
            <Link to="/about" className="nav-btn btn-about">About</Link>
            <Link to="/login" className="nav-btn btn-login">Login</Link>
            <Link to="/register" className="nav-btn btn-register">Register</Link>
        </div>
    );
}

export function AuthenticationNavBar({ name }: { name: string }) {
    return (
        <div className="nav-links">
            <span style={{color: 'white', alignSelf: 'center', marginRight: '10px'}}>
                Olá, {name}
            </span>
            <Link to="/about" className="nav-btn btn-about">About</Link>
            <Link to="/invitations" className="nav-btn btn-invitations">Invite</Link>
            <Link to="/lobbies" className="nav-btn btn-register">Jogar</Link>
            <Link to="/profile" className="nav-btn btn-login">Perfil</Link>
            <Link to="/logout" className="nav-btn btn-logout">Sair</Link>
        </div>
    );
}

export function NavBarBase({ children }: { children: React.ReactNode }) {
    return (
        <nav className="navbar">
            <Link to="/" className="logo-text" style={{ textDecoration: 'none' }}>
                🎲 PokerDice</Link>
            {children}
        </nav>
    );
}