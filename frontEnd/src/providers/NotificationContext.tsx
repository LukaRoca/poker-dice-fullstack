import React, { createContext, useContext, useState, type ReactNode } from 'react';

export interface Notification {
    id: number;
    message: string;
    type: 'info' | 'success' | 'warning' | 'error';
}

interface NotificationContextType {
    addNotification: (message: string, type?: 'info' | 'success' | 'warning' | 'error') => void;
    notifications: Notification[];
    removeNotification: (id: number) => void;
}

const NotificationContext = createContext<NotificationContextType | undefined>(undefined);

export const NotificationProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
    const [notifications, setNotifications] = useState<Notification[]>([]);

    const addNotification = (message: string, type: 'info' | 'success' | 'warning' | 'error' = 'info') => {
        const id = Date.now();
        const newNotif = { id, message, type };

        setNotifications(prev => [...prev, newNotif]);

        setTimeout(() => {
            removeNotification(id);
        }, 5000);
    };

    const removeNotification = (id: number) => {
        setNotifications(prev => prev.filter(n => n.id !== id));
    };

    return (
        <NotificationContext.Provider value={{ addNotification, notifications, removeNotification }}>
            {children}
        </NotificationContext.Provider>
    );
};

export const useNotifications = () => {
    const context = useContext(NotificationContext);
    if (!context) {
        throw new Error('useNotifications deve ser usado dentro de um NotificationProvider');
    }
    return context;
};