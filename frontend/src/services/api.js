import axios from 'axios';

/**
 * Instancia central de axios.
 *
 * Toda llamada al backend pasa por aquí: ni las páginas ni los componentes
 * importan axios directamente, porque los dos interceptores de abajo son los
 * que aportan la sesión y la traducción de errores.
 *
 * La URL base se lee de VITE_API_URL, que Vite resuelve al construir:
 *   - desarrollo: http://localhost:8080/api
 *   - producción: /api  (mismo origen, el reverse proxy enruta al backend)
 */
const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
    headers: {
        'Content-Type': 'application/json'
    }
});

/**
 * Token JWT de la sesión guardada.
 *
 * La sesión completa vive en localStorage bajo la clave `user`, tal como la
 * dejó el login, y el token es uno de sus campos.
 */
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

/*
 * Petición: adjunta la credencial.
 *
 * El token se lee en cada llamada y no al crear la instancia, de modo que
 * iniciar o cerrar sesión surte efecto de inmediato sin recargar la página.
 * Sin token la petición sale igual: los endpoints públicos, como el propio
 * login, no necesitan cabecera.
 */
api.interceptors.request.use(
    (config) => {
        const token = obtenerToken();
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }

        return config;
    },
    (error) => Promise.reject(error)
);

/*
 * Respuesta: desempaqueta el cuerpo y normaliza los errores.
 */
api.interceptors.response.use(
    (response) => {
        // El backend envuelve sus respuestas en { success, message, data }. Se
        // desempaqueta aquí para que las pantallas lean el dato directamente en
        // `response.data`, y el texto que acompaña queda en `response.mensaje`.
        //
        // Los PDF y los Excel llegan como blob y no llevan envoltorio: se
        // detectan por la ausencia de esos campos y se dejan intactos.
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

        // Todo error sale de aquí con un `mensaje` listo para mostrar, que es
        // el que lee `notificarError`. El backend garantiza que su `message`
        // nunca contiene trazas ni detalles internos.
        error.mensaje =
            error.response?.data?.message ||
            'No se pudo completar la operación. Intenta de nuevo.';

        if (status === 401) {
            // Sin sesión o sesión caducada: se descarta la guardada y se manda
            // al login. La comprobación de ruta evita el bucle de redirección
            // cuando el propio login responde 401 por credenciales erróneas.
            localStorage.removeItem('user');
            if (window.location.pathname !== '/login') {
                window.location.href = '/login';
            }
        } else if (status === 403) {
            // Un 403 no cierra la sesión: el usuario está identificado, pero esa
            // acción concreta no le corresponde por rol. Expulsarlo lo dejaría
            // fuera cada vez que toca algo ajeno a su perfil.
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
