import api from './api';

const RUTA = '/correos';

/**
 * Directorio de correos institucionales. Espeja CorreoInstitucionalResource.
 *
 * Exige rol SOPORTE o ADMINISTRADOR; el borrado definitivo solo ADMINISTRADOR.
 *
 * Igual que en el taller, la versión anterior envolvía los errores en
 * `new Error(...)` leyendo `error.response.data.mensaje`, campo que el backend
 * no usa: todos los fallos mostraban el texto genérico de respaldo. Ahora se
 * propagan y los traduce el interceptor de api.js.
 */
export const correoService = {
    /** Directorio paginado, con búsqueda y filtro de estado en la base. */
    getAll: (params = {}) => api.get(RUTA, { params }),

    /** Catálogo de estados de la cuenta, servido desde el enum. */
    getEstados: () => api.get(`${RUTA}/estados`),

    getById: (id) => api.get(`${RUTA}/${id}`),

    create: (data) => api.post(RUTA, data),
    update: (id, data) => api.put(`${RUTA}/${id}`, data),

    /**
     * Alta, baja o suspensión de la cuenta ante el proveedor. Es el único
     * camino para cambiar el estado: la edición normal ya no lo toca.
     */
    cambiarEstado: (id, estado) => api.patch(`${RUTA}/${id}/estado`, { estado }),

    /** Borrado definitivo. Para dar de baja sin perder historial, usa el estado. */
    delete: (id) => api.delete(`${RUTA}/${id}`),
};

export default correoService;
