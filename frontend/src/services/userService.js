import api from './api';

/**
 * Operaciones sobre usuarios. Espeja UsuarioResource del backend.
 *
 * Todas exigen rol ADMINISTRADOR: si las invoca otro rol, el backend responde
 * 403 y el interceptor de api.js lo traduce a un mensaje legible sin cerrar la
 * sesión.
 */
export const userService = {
    /**
     * Listado paginado del panel. Recibe el objeto que arma `useTablaPaginada`
     * ({ page, size, sort, busqueda, ...filtros }) y devuelve un `PageResponse`.
     *
     * Antes traía la tabla completa y el filtrado ocurría en el navegador, de
     * modo que la búsqueda solo miraba lo ya descargado.
     */
    getAll: (params = {}) => api.get('/v1/admin/usuarios', { params }),

    /** Catálogo completo sin paginar, para selectores. */
    getTodos: () => api.get('/v1/admin/usuarios/todos'),

    /** Personal de soporte, para asignación manual y soporte fijo de un área. */
    getSoporte: () => api.get('/v1/admin/usuarios/soporte'),

    create: (data) => api.post('/v1/admin/usuarios', data),
    update: (id, data) => api.put(`/v1/admin/usuarios/${id}`, data),

    /** Genera una contraseña temporal nueva y obliga a cambiarla al entrar. */
    resetPassword: (id) => api.put(`/v1/admin/usuarios/${id}/reset-password`),

    /** Alterna entre activo e inactivo. Es una baja lógica: conserva historial. */
    toggleEstado: (id) => api.put(`/v1/admin/usuarios/${id}/toggle-estado`),

    /**
     * Activa o desactiva la recepción automática de tickets de un técnico.
     *
     * El backend espera el campo `disponible` (ver `DisponibilidadRequest`).
     * La pantalla anterior enviaba `disponibleSoporte`, que el DTO no conoce,
     * así que el interruptor respondía siempre con un error de validación y la
     * disponibilidad nunca llegaba a cambiar.
     */
    actualizarDisponibilidad: (id, disponible) =>
        api.patch(`/v1/admin/usuarios/${id}/disponibilidad`, { disponible }),
};

/**
 * Catálogo de roles.
 *
 * La pantalla los tenía escritos a mano con los ids 4, 5 y 6, que no
 * corresponden a los de la base de datos (1, 2 y 3): crear un usuario fallaba
 * siempre con "El rol seleccionado no existe".
 */
export const rolService = {
    getAll: () => api.get('/v1/admin/roles'),
};

export default userService;
