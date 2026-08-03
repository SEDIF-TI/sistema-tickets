import { useContext, useEffect, useMemo, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { AuthContext } from './AuthContext';
import { WebSocketContext } from './webSocketContextObject.js';

/**
 * URL del endpoint SockJS.
 *
 * Se deriva de VITE_API_URL quitándole el sufijo `/api`, en lugar de apuntar a
 * `http://localhost:8080` fijo como antes: con la URL escrita a mano, el
 * tiempo real solo funcionaba en el equipo del desarrollador y en producción
 * el panel de soporte se quedaba mudo sin ningún aviso.
 */
const urlSocket = () => {
    const base = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
    // `/api` puede ser una ruta relativa detrás del reverse proxy, así que se
    // resuelve contra el origen actual antes de recortar.
    const absoluta = new URL(base, window.location.origin);
    absoluta.pathname = `${absoluta.pathname.replace(/\/api\/?$/, '')}/ws-tickets`;
    return absoluta.toString();
};

export const WebSocketProvider = ({ children }) => {
    const { user } = useContext(AuthContext);
    const [isConnected, setIsConnected] = useState(false);

    // El cliente se construye durante el render y no dentro del efecto.
    //
    // La versión anterior lo creaba en el efecto y lo publicaba con
    // `setStompClient`, lo que provocaba un segundo render en cada conexión y,
    // sobre todo, dejaba a los consumidores con `stompClient` a null durante el
    // primer render: las pantallas que se suscriben en su propio efecto se lo
    // encontraban vacío y se quedaban sin suscribir hasta el siguiente ciclo.
    //
    // Solo el token identifica la sesión del canal: usar el objeto `user`
    // completo reconstruiría el socket cada vez que cambiara cualquier otro
    // campo del perfil.
    const stompClient = useMemo(() => {
        if (!user?.token) return null;

        return new Client({
            webSocketFactory: () => new SockJS(urlSocket()),

            // El token JWT viaja en la cabecera de conexión STOMP.
            connectHeaders: { Authorization: `Bearer ${user.token}` },

            reconnectDelay: 5000,   // reconexión automática si la red falla
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,

            onConnect: () => setIsConnected(true),
            onWebSocketClose: () => setIsConnected(false),

            // Los volcados a consola se limitan al modo desarrollo: en
            // producción el cuerpo de un frame STOMP puede contener datos de
            // tickets, y la consola del navegador es visible para cualquiera
            // con acceso al equipo.
            onStompError: (frame) => {
                if (import.meta.env.DEV) {
                    console.error('Error de broker STOMP:', frame.headers?.message);
                }
            },
        });
    }, [user?.token]);

    useEffect(() => {
        if (!stompClient) return;

        stompClient.activate();

        // Cierra el socket al desmontar o al cambiar de sesión.
        return () => {
            stompClient.deactivate();
        };
    }, [stompClient]);

    // Sin cliente no puede haber conexión. Se deriva aquí en lugar de
    // reiniciar el estado al cerrar sesión: así no queda un `isConnected` en
    // true durante el render en que el socket ya desapareció.
    const valor = useMemo(
        () => ({ stompClient, isConnected: Boolean(stompClient) && isConnected }),
        [stompClient, isConnected]
    );

    return (
        <WebSocketContext.Provider value={valor}>
            {children}
        </WebSocketContext.Provider>
    );
};

// El contexto vive en ./webSocketContextObject.js y el hook `useWebSocket` en
// ./useWebSocket.js: mantenerlos aquí rompía la recarga en caliente y cada
// retoque de este archivo tiraba la conexión abierta.