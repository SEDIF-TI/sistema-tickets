import api from './api';

/**
 * Autenticación y operaciones sobre la propia cuenta.
 * Espeja AuthController (security/auth) y PerfilResource del backend.
 */
export const authService = {
    /** Devuelve los datos de sesión: token, rol, vistas del menú y área. */
    login: (identificador, password) =>
        api.post('/v1/auth/login', { identificador, password }),

    /**
     * Cambia la contraseña del usuario autenticado.
     *
     * El backend exige ahora la contraseña actual: sin ella, cualquiera con un
     * token robado podía apoderarse de la cuenta. La ruta pasó de
     * /v1/admin/usuarios/password a /v1/perfil/password, porque es una
     * operación personal y no de administración.
     */
    cambiarPassword: (passwordActual, nuevaPassword) =>
        api.put('/v1/perfil/password', { passwordActual, nuevaPassword })
};
