import { useContext, useEffect, useMemo, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { AuthContext } from './AuthContext';
import { WebSocketContext } from './webSocketContextObject.js';

/**
 * URL del endpoint SockJS `/ws-tickets`.
 *
 * Se deriva de VITE_API_URL recortándole el sufijo `/api`, de modo que el canal
 * de tiempo real apunte al mismo servidor que el resto de la API en cualquier
 * entorno, sin una segunda variable que mantener.
 */
const urlSocket = () => {
    const base = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
    // En producción VITE_API_URL es la ruta relativa `/api`, que SockJS no
    // acepta: se resuelve contra el origen actual para obtener una absoluta.
    const absoluta = new URL(base, window.location.origin);
    absoluta.pathname = `${absoluta.pathname.replace(/\/api\/?$/, '')}/ws-tickets`;
    return absoluta.toString();
};

/**
 * Canal de tiempo real sobre STOMP.
 *
 * Mantiene una sola conexión para toda la aplicación y la publica en
 * `{ stompClient, isConnected }`. Las pantallas no crean clientes propios: se
 * suscriben a sus destinos con `stompClient.subscribe(destino, callback)` desde
 * un efecto que depende de `isConnected`, y devuelven `unsubscribe` en la
 * limpieza. El servidor emite, entre otros, `/topic/alertas-resguardos`, del
 * que se ocupa MainLayout.
 *
 * La conexión solo existe mientras hay sesión: sin token no se construye
 * cliente, y cerrar sesión desactiva el que hubiera.
 */
export const WebSocketProvider = ({ children }) => {
    const { user } = useContext(AuthContext);
    const [isConnected, setIsConnected] = useState(false);

    // El cliente se construye durante el render, no dentro del efecto, para que
    // los consumidores lo tengan ya disponible en su primer ciclo: una pantalla
    // que se suscribe en su propio efecto se encontraría `stompClient` a null y
    // se quedaría sin suscribir hasta el render siguiente.
    //
    // La dependencia es el token y no el objeto `user` completo, porque es lo
    // único que identifica la sesión del canal: cualquier otro cambio del
    // perfil reconstruiría el socket sin motivo.
    const stompClient = useMemo(() => {
        if (!user?.token) return null;

        return new Client({
            webSocketFactory: () => new SockJS(urlSocket()),

            // El JWT viaja en el frame CONNECT de STOMP, no en una cabecera
            // HTTP: el handshake de SockJS no admite cabeceras propias, así que
            // el servidor autentica el canal al recibir esta conexión.
            connectHeaders: { Authorization: `Bearer ${user.token}` },

            reconnectDelay: 5000,   // reconexión automática si la red falla
            // Latidos en ambos sentidos: detectan una conexión muerta que el
            // socket todavía da por abierta.
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,

            onConnect: () => setIsConnected(true),
            onWebSocketClose: () => setIsConnected(false),

            // El volcado se limita al modo desarrollo: el cuerpo de un frame
            // STOMP puede contener datos de tickets, y la consola del navegador
            // es visible para cualquiera con acceso al equipo.
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

    // `isConnected` se combina con la existencia del cliente en lugar de
    // reiniciar el estado al cerrar sesión: así no queda expuesto en `true`
    // durante el render en que el socket ya desapareció.
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

// Este archivo exporta únicamente el proveedor. El objeto de contexto vive en
// ./webSocketContextObject.js y el hook `useWebSocket` en ./useWebSocket.js,
// porque la recarga en caliente de Vite solo conserva el estado de los módulos
// que exportan componentes: con las tres cosas juntas, cualquier retoque
// recargaba la aplicación entera y tiraba la conexión abierta.