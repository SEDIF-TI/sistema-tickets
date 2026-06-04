import { createContext, useContext, useEffect, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { AuthContext } from './AuthContext';

export const WebSocketContext = createContext();

export const WebSocketProvider = ({ children }) => {
    // Extraemos el usuario logueado de su contexto de autenticación existente
    const { user } = useContext(AuthContext);
    const [stompClient, setStompClient] = useState(null);
    const [isConnected, setIsConnected] = useState(false);

    useEffect(() => {
        // Regla de negocio: Si no hay usuario o no hay token, no intentamos conectar
        if (!user || !user.token) {
            return;
        }

        const client = new Client({
            // Ruta de su backend
            webSocketFactory: () => new SockJS('http://localhost:8080/ws-tickets'),
            
            // Pasamos el token JWT en las cabeceras por seguridad
            connectHeaders: {
                Authorization: `Bearer ${user.token}`
            },
            
            reconnectDelay: 5000, // Intento de reconexión automática si el internet falla
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        client.onConnect = (frame) => {
            console.log('Conectado a WebSockets con éxito');
            setIsConnected(true);
        };

        client.onStompError = (frame) => {
            console.error('Error de Broker STOMP: ', frame.headers['message']);
            console.error('Detalles: ', frame.body);
        };

        client.onWebSocketClose = () => {
            console.log('Conexión WebSocket cerrada');
            setIsConnected(false);
        };

        // Iniciamos la conexión
        client.activate();
        setStompClient(client);

        // Limpieza: Desconecta el socket si el componente se desmonta o el usuario cambia
        return () => {
            if (client.active) {
                client.deactivate();
            }
        };
    }, [user]); // Este Hook se vuelve a ejecutar automáticamente si el usuario hace login o logout

    return (
        <WebSocketContext.Provider value={{ stompClient, isConnected }}>
            {children}
        </WebSocketContext.Provider>
    );
};

// Hook personalizado para usar el WebSocket en cualquier pantalla
export const useWebSocket = () => {
    return useContext(WebSocketContext);
};