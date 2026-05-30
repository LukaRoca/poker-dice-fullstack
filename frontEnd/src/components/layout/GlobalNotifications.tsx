import React from 'react';
import { useNotifications } from '../../providers/NotificationContext';
import '../../styles/Notifications.css';

export const GlobalNotifications: React.FC = () => {
    const { notifications, removeNotification } = useNotifications();

    if (notifications.length === 0) return null;

    return (
        <div className="notification-container">
            {notifications.map(notif => (
                <div key={notif.id} className={`notification-toast notification-${notif.type}`}>
                    <p>{notif.message}</p>
                    <button onClick={() => removeNotification(notif.id)} className="notification-close">
                        ✕
                    </button>
                </div>
            ))}
        </div>
    );
};