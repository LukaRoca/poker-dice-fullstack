import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuthentication } from '../../providers/AuthenticationProvider';

export const RequireAuthentication: React.FC<{ children?: React.ReactNode }> = ({ children }) => {
    const [ user ] = useAuthentication();
    const loading = false;

    if (loading) {
        return <div>Loading...</div>;
    }

    if (!user) {
        return <Navigate to="/login" replace />;
    }
    return children ? <>{children}</> : <Outlet />;
};

export default RequireAuthentication;