import { createContext, useContext, useState, useCallback } from 'react';
import { Snackbar, Alert } from '@mui/material';

/**
 * Notificaciones globales (toasts).
 *
 * Un solo Snackbar para toda la aplicación, montado por el proveedor: las
 * pantallas solo piden el mensaje.
 *
 *   const { notificar, notificarError } = useNotification();
 *   notificar('Ticket creado correctamente.');
 *
 * Hay una función por severidad —éxito, información, advertencia y error—, y
 * solo se muestra un aviso a la vez: el siguiente sustituye al anterior. Los
 * errores permanecen el doble de tiempo en pantalla, porque suelen exigir una
 * decisión y no solo confirmar algo que ya se sabía.
 */
const NotificationContext = createContext(null);

export const NotificationProvider = ({ children }) => {
    const [notificacion, setNotificacion] = useState({
        abierta: false,
        mensaje: '',
        severidad: 'success'
    });

    const mostrar = useCallback((mensaje, severidad = 'success') => {
        setNotificacion({ abierta: true, mensaje, severidad });
    }, []);

    const notificar = useCallback((mensaje) => mostrar(mensaje, 'success'), [mostrar]);
    const notificarInfo = useCallback((mensaje) => mostrar(mensaje, 'info'), [mostrar]);
    const notificarAdvertencia = useCallback((mensaje) => mostrar(mensaje, 'warning'), [mostrar]);

    /**
     * Muestra un error. Es lo que va en cada `catch`: acepta el error de axios
     * tal cual —el interceptor de api.js ya le dejó el texto legible en
     * `mensaje`— o directamente una cadena.
     */
    const notificarError = useCallback((error) => {
        const mensaje =
            typeof error === 'string'
                ? error
                : error?.mensaje ||
                  error?.response?.data?.message ||
                  'Ocurrió un error inesperado.';
        mostrar(mensaje, 'error');
    }, [mostrar]);

    const cerrar = useCallback((_evento, motivo) => {
        // No se cierra al hacer clic fuera: el usuario debe poder leerlo.
        if (motivo === 'clickaway') return;
        setNotificacion((actual) => ({ ...actual, abierta: false }));
    }, []);

    return (
        <NotificationContext.Provider
            value={{ notificar, notificarError, notificarInfo, notificarAdvertencia }}
        >
            {children}
            <Snackbar
                open={notificacion.abierta}
                autoHideDuration={notificacion.severidad === 'error' ? 8000 : 4000}
                onClose={cerrar}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
            >
                <Alert
                    onClose={cerrar}
                    severity={notificacion.severidad}
                    variant="filled"
                    sx={{ width: '100%' }}
                >
                    {notificacion.mensaje}
                </Alert>
            </Snackbar>
        </NotificationContext.Provider>
    );
};

/** Acceso a las notificaciones desde cualquier componente. */
export const useNotification = () => {
    const contexto = useContext(NotificationContext);
    if (!contexto) {
        throw new Error('useNotification debe usarse dentro de <NotificationProvider>.');
    }
    return contexto;
};
