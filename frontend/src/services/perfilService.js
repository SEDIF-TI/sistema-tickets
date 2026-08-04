import api from './api';

/**
 * Operaciones del usuario sobre su propia cuenta.
 * Espeja PerfilResource del backend.
 *
 * La pantalla de perfil llamaba a `PUT /v1/admin/usuarios/password`, ruta que
 * dejó de existir cuando el cambio de contraseña se trasladó a `/v1/perfil`
 * para sacarlo del área de administración. El resultado era que **nadie podía
 * cambiar su contraseña**, ni siquiera en el primer acceso, que es obligatorio.
 */
export const perfilService = {
    /**
     * Cambia la contraseña del usuario de la sesión.
     *
     * El backend exige también la contraseña actual: sin ella, cualquiera que
     * consiguiera un token podría apoderarse de la cuenta de forma permanente.
     * En el primer acceso, la actual es la temporal recién entregada.
     */
    cambiarPassword: (passwordActual, nuevaPassword) =>
        api.put('/v1/perfil/password', { passwordActual, nuevaPassword }),

    /**
     * Vistas del menú vigentes para la sesión.
     *
     * El menú se guarda en el navegador al iniciar sesión, así que sin esta
     * consulta una vista retirada seguía dibujándose hasta cerrar sesión —y un
     * permiso recién concedido no aparecía—.
     */
    getVistas: () => api.get('/v1/perfil/vistas'),
};

export default perfilService;
