import { useContext } from 'react';
import { WebSocketContext } from './webSocketContextObject.js';

/**
 * Acceso al canal de tiempo real desde cualquier pantalla.
 *
 * Vive en su propio archivo y no junto al proveedor porque Vite solo aplica
 * recarga en caliente a los módulos que exportan componentes: al convivir el
 * hook con el proveedor, cada cambio en el archivo recargaba la aplicación
 * entera y cerraba la conexión.
 *
 * Devuelve `{ stompClient, isConnected }`. Fuera del proveedor devuelve un
 * objeto con valores inertes, para que un componente montado sin él no rompa.
 */
export const useWebSocket = () => useContext(WebSocketContext) ?? {
    stompClient: null,
    isConnected: false,
};

export default useWebSocket;
