import api from './api';

/**
 * Operaciones sobre áreas. Espeja AreaResource del backend.
 *
 * Todas exigen rol ADMINISTRADOR.
 */
export const areaService = {
    /**
     * Listado paginado del panel. Recibe el objeto que arma `useTablaPaginada`
     * ({ page, size, sort, busqueda, ...filtros }) y devuelve un `PageResponse`.
     */
    getAll: (params = {}) => api.get('/v1/admin/areas', { params }),

    /**
     * Catálogo completo sin paginar, para los selectores de área de otros
     * formularios (el alta de usuario, por ejemplo), que necesitan la lista
     * entera y no una página.
     */
    getTodas: () => api.get('/v1/admin/areas/todas'),

    create: (data) => api.post('/v1/admin/areas', data),
    update: (id, data) => api.put(`/v1/admin/areas/${id}`, data),

    /** Baja lógica. El backend la rechaza si el área todavía tiene personal. */
    delete: (id) => api.delete(`/v1/admin/areas/${id}`),

    /**
     * Fija el técnico responsable del área, o lo retira si se pasa `null`.
     *
     * Retirarlo no era posible antes: una vez asignado un técnico fijo, no
     * había forma de devolver el área al balanceador automático.
     */
    asignarSoporteFijo: (id, soporteFijoId) =>
        api.patch(`/v1/admin/areas/${id}/soporte-fijo`, { soporteFijoId }),
};

export default areaService;
