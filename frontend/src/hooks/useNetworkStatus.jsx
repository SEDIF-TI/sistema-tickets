import { useState, useEffect, useCallback, useRef } from 'react';

/**
 * Detección de conectividad con el servidor.
 *
 * La versión anterior dependía solo de `navigator.onLine`, que informa de si
 * hay interfaz de red activa — no de si el backend responde. Da falsos
 * positivos habituales en la oficina: el equipo sigue conectado al wifi, pero
 * el servidor está caído, el cable del rack se soltó o el túnel VPN cayó.
 * `navigator.onLine` seguiría diciendo "en línea" mientras el usuario ve
 * errores sin entender por qué.
 *
 * Ahora se combinan dos señales:
 *
 *  1. Los eventos `online`/`offline` del navegador: inmediatos y gratuitos
 *     cuando el equipo pierde la red por completo.
 *  2. Un sondeo ligero al backend, que es el único que confirma que el
 *     servidor responde de verdad.
 *
 * El sondeo solo corre cuando el navegador se declara en línea, y espacia los
 * intentos: no tiene sentido insistir cada pocos segundos si el servidor lleva
 * rato caído.
 *
 * Devuelve un booleano (`true` = sin conexión utilizable) con propiedades
 * adicionales, de modo que el uso existente `const isOffline =
 * useNetworkStatus()` sigue funcionando sin cambios.
 */

/** Cada cuánto se comprueba el servidor mientras todo va bien. */
const INTERVALO_NORMAL_MS = 30000;

/** Cada cuánto se reintenta cuando se ha detectado una caída. */
const INTERVALO_REINTENTO_MS = 10000;

/** Tiempo máximo de espera del sondeo antes de darlo por fallido. */
const TIMEOUT_SONDEO_MS = 5000;

export const useNetworkStatus = () => {
    // Estado del navegador: hay o no interfaz de red.
    const [sinRed, setSinRed] = useState(!navigator.onLine);

    // Estado del servidor: responde o no. Se asume que sí hasta comprobar lo
    // contrario, para no mostrar una alarma nada más cargar la página.
    const [servidorCaido, setServidorCaido] = useState(false);

    const temporizador = useRef(null);

    /**
     * Comprueba que el backend responde.
     *
     * Se acepta cualquier respuesta HTTP como señal de vida: incluso un 401 o
     * un 404 significan que el servidor está en pie, que es lo único que
     * interesa aquí.
     */
    const comprobarServidor = useCallback(async () => {
        if (!navigator.onLine) {
            setServidorCaido(false); // el problema es la red, no el servidor
            return;
        }

        const controlador = new AbortController();
        const corte = setTimeout(() => controlador.abort(), TIMEOUT_SONDEO_MS);

        try {
            // Se sondea /salud y no la raíz de la API.
            //
            // `/api` a secas no corresponde a ningún controlador: Spring
            // Security lo rechazaba con 403 ANTES de que el filtro de CORS
            // añadiera sus cabeceras, así que el navegador lo reportaba como
            // un error de CORS. La consola se llenaba de "blocked by CORS
            // policy" cada treinta segundos y la aplicación mostraba "No hay
            // comunicación con el servidor" aunque el backend estuviera en pie.
            const base = import.meta.env.VITE_API_URL || '/api';
            await fetch(`${base.replace(/\/$/, '')}/salud`, {
                method: 'HEAD',
                cache: 'no-store',
                signal: controlador.signal,
            });
            setServidorCaido(false);
        } catch {
            // Falla la petición: servidor caído, DNS roto o red intermitente.
            setServidorCaido(true);
        } finally {
            clearTimeout(corte);
        }
    }, []);

    // --- Eventos del navegador ---
    useEffect(() => {
        const alConectar = () => {
            setSinRed(false);
            comprobarServidor(); // confirmar de inmediato, sin esperar al ciclo
        };
        const alDesconectar = () => setSinRed(true);

        window.addEventListener('online', alConectar);
        window.addEventListener('offline', alDesconectar);

        return () => {
            window.removeEventListener('online', alConectar);
            window.removeEventListener('offline', alDesconectar);
        };
    }, [comprobarServidor]);

    // --- Sondeo periódico ---
    useEffect(() => {
        const programar = () => {
            const espera = servidorCaido ? INTERVALO_REINTENTO_MS : INTERVALO_NORMAL_MS;
            temporizador.current = setTimeout(async () => {
                await comprobarServidor();
                programar();
            }, espera);
        };

        comprobarServidor();
        programar();

        return () => clearTimeout(temporizador.current);
    }, [comprobarServidor, servidorCaido]);

    const sinConexion = sinRed || servidorCaido;

    /*
     * Se devuelve un objeto que se comporta como booleano gracias a valueOf:
     * `if (isOffline)` y `isOffline && ...` siguen funcionando igual que
     * antes, y además quedan disponibles los detalles para poder distinguir
     * "no hay internet" de "el servidor no responde".
     */
    return {
        valueOf: () => sinConexion,
        sinConexion,
        sinRed,
        servidorCaido,
        /** Fuerza una comprobación inmediata (botón "reintentar"). */
        reintentar: comprobarServidor,
    };
};

export default useNetworkStatus;
