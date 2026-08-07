import { useContext } from 'react';
import { WebSocketContext } from './webSocketContextObject.js';

/**
 * Acceso al canal de tiempo real desde cualquier pantalla.
 *
 * Devuelve `{ stompClient, isConnected }`. Fuera del proveedor devuelve esos
 * mismos campos con valores inertes en lugar de lanzar, de modo que un
 * componente montado sin canal —una pantalla aislada, una prueba— simplemente
 * no se suscribe a nada.
 *
 * Vive en su propio archivo y no junto al proveedor porque la recarga en
 * caliente de Vite solo conserva el estado de los módulos que exportan
 * componentes: compartiendo archivo, cada cambio tiraba la conexión abierta.
 */
export const useWebSocket = () => useContext(WebSocketContext) ?? {
    stompClient: null,
    isConnected: false,
};

export default useWebSocket;
