import { createContext } from 'react';

/**
 * Objeto de contexto del canal de tiempo real. Su valor es
 * `{ stompClient, isConnected }`, que publica `WebSocketProvider` y consume el
 * hook `useWebSocket`.
 *
 * Vive separado del proveedor porque la recarga en caliente de Vite solo
 * conserva el estado de los módulos que exportan únicamente componentes:
 * compartiendo archivo, cualquier retoque tiraba la conexión abierta.
 */
export const WebSocketContext = createContext(null);

export default WebSocketContext;
