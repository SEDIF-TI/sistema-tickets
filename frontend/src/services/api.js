import axios from 'axios';

/**
 * Instancia central de axios.
 *
 * Toda llamada al backend pasa por aquí: ni las páginas ni los componentes
 * deben importar axios directamente.
 *
 * La URL base se lee de VITE_API_URL, que Vite resuelve al construir:
 *   - desarrollo: http://localhost:8080/api
 *   - producción: /api  (mismo origen, el reverse proxy enruta al backend)
 * Antes estaba escrita a mano, lo que obligaba a editar el código para
 * desplegar en otro entorno.
 */
const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
    headers: {
        'Content-Type': 'application/json'
    }
});

/** Lee el token guardado en la sesión del navegador. */
const obtenerToken = () => {
    const usuarioGuardado = localStorage.getItem('user');
    if (!usuarioGuardado) return null;

    try {
        const usuario = JSON.parse(usuarioGuardado);
        return usuario.token ?? null;
    } catch {
        // Sesión corrupta: se descarta en lugar de arrastrar el error.
        localStorage.removeItem('user');
        return null;
    }
};

// --- Petición: adjunta el token en cada llamada ---------------------------
api.interceptors.request.use(
    (config) => {
        const token = obtenerToken();
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }

        // Se eliminaron los console.log que imprimían cada petición y si el
        // token viajaba: cualquiera con la consola abierta veía el mapa de la
        // API y el estado de la sesión. En producción, además, ensucian la
        // consola del usuario.

        return config;
    },
    (error) => Promise.reject(error)
);

// --- Respuesta ------------------------------------------------------------
api.interceptors.response.use(
    (response) => {
        // El backend envuelve todo en { success, message, data }. Aquí se
        // desempaqueta para que las pantallas sigan leyendo `response.data`
        // como antes y no haya que tocarlas una por una.
        //
        // Los PDF y los Excel se descargan como blob y no llevan envoltorio:
        // se dejan intactos.
        const cuerpo = response.data;
        const esEnvoltorio =
            cuerpo &&
            typeof cuerpo === 'object' &&
            !(cuerpo instanceof Blob) &&
            'success' in cuerpo &&
            'data' in cuerpo;

        if (esEnvoltorio) {
            response.mensaje = cuerpo.message;
            response.data = cuerpo.data;
        }

        return response;
    },
    (error) => {
        const status = error.response?.status;

        // Mensaje legible para la interfaz. El backend ya se encarga de que
        // nunca contenga trazas ni detalles internos.
        error.mensaje =
            error.response?.data?.message ||
            'No se pudo completar la operación. Intenta de nuevo.';

        if (status === 401) {
            // 401 = sin sesión o sesión caducada: hay que volver a entrar.
            localStorage.removeItem('user');
            if (window.location.pathname !== '/login') {
                window.location.href = '/login';
            }
        } else if (status === 403) {
            // 403 NO cierra la sesión: el usuario está identificado pero no
            // tiene permiso para esa acción concreta. La versión anterior
            // trataba ambos códigos igual y expulsaba al usuario cada vez que
            // tocaba algo fuera de su rol.
            error.mensaje =
                error.response?.data?.message ||
                'No tienes permisos para realizar esta acción.';
        } else if (status === 429) {
            // Límite de intentos del backend (anti fuerza bruta).
            error.mensaje =
                error.response?.data?.message ||
                'Demasiados intentos. Espera unos minutos e inténtalo de nuevo.';
        } else if (!error.response) {
            // Sin respuesta: servidor caído, red interrumpida o petición
            // cancelada por tiempo de espera.
            error.mensaje =
                'No hay conexión con el servidor. Verifica tu red e inténtalo de nuevo.';
        }

        return Promise.reject(error);
    }
);

export default api;
