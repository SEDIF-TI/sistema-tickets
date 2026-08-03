import { createContext } from 'react';

/**
 * Contexto del canal de tiempo real.
 *
 * Vive separado del proveedor porque Vite solo aplica recarga en caliente a los
 * módulos que exportan únicamente componentes: con el contexto y el proveedor
 * en el mismo archivo, cualquier retoque recargaba la aplicación entera y
 * tiraba la conexión abierta.
 *
 * Valor: `{ stompClient, isConnected }`.
 */
export const WebSocketContext = createContext(null);

export default WebSocketContext;
