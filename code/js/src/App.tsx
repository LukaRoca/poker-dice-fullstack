import {createBrowserRouter, Outlet, RouterProvider} from 'react-router-dom';
import {AuthenticationProvider} from './providers/AuthenticationProvider';
import {RequireAuthentication} from './components/auth/RequireAuthentication';

import {AboutPage} from './pages/AboutPage';
import {HomePage} from './components/layout/HomePage.tsx';
import {InvitationsPage} from './pages/InvitationsPage';
import {LobbiesPage} from './components/lobbies/LobbiesPage.tsx';
import {LobbyCreationPage} from './components/lobbies/LobbyCreationPage.tsx';
import {LobbyDetailPage} from './components/lobbies/LobbyDetailPage.tsx';
import {LoginPage} from './components/auth/LoginPage.tsx';
import {LogoutPage} from './pages/LogoutPage';
import {MatchPage} from './pages/MatchPage';
import {ProfilePage} from './pages/ProfilePage';
import {RegisterPage} from './components/auth/RegisterPage.tsx';
import {ErrorPage} from './components/error/ErrorPage.tsx';
import {GlobalMatchRedirect} from "./components/layout/MatchRedirect.tsx";

import { GlobalNotifications } from './components/layout/GlobalNotifications';
import { SSEEmitterProvider } from "./providers/SSEContext.tsx";
import { NotificationProvider } from './providers/NotificationContext';

function RootLayout() {
    return (
        <>
            <GlobalNotifications />
            <GlobalMatchRedirect />
            <Outlet />
        </>
    );
}

const router = createBrowserRouter([
    {
        element: <RootLayout />,
        errorElement: <ErrorPage/>,
        children: [
            {path: '/', element: <HomePage/>},
            {path: '/login', element: <LoginPage/>},
            {path: '/register/:inviteCode?', element: <RegisterPage/>},
            {path: '/about', element: <AboutPage/>},

            {
                element: <RequireAuthentication/>,
                children: [
                    {path: '/profile', element: <ProfilePage/>},
                    {path: '/invitations', element: <InvitationsPage/>},
                    {path: '/logout', element: <LogoutPage/>},

                    {path: '/lobbies', element: <LobbiesPage/>},
                    {path: '/lobbies/create', element: <LobbyCreationPage/>},
                    {path: '/lobby/:lobbyId', element: <LobbyDetailPage/>},
                    {path: '/match/:matchId', element: <MatchPage/>},
                ]
            }
        ]
    }
]);


export function App() {
    return (
        <AuthenticationProvider>
            <SSEEmitterProvider>
                <NotificationProvider>
                    <RouterProvider router={router}/>
                </NotificationProvider>
            </SSEEmitterProvider>
        </AuthenticationProvider>
    )
}