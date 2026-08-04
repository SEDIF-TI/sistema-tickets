import api from './api';

/**
 * Catálogo de equipos. Espeja EquipoResource del backend.
 *
 * Sirve para no volver a teclear marca y modelo en cada dictamen técnico.
 * Lectura y alta están abiertas a SOPORTE y ADMINISTRADOR; editar y borrar
 * solo a ADMINISTRADOR.
 */
export const equipoService = {
    /** Listado paginado del catálogo, con búsqueda resuelta en la base. */
    getAll: (params = {}) => api.get('/v1/equipos', { params }),

    /** Sugerencias para el autocompletado del formulario de dictamen. */
    buscar: (q) => api.get('/v1/equipos/buscar', { params: { q } }),

    /**
     * Alta, o actualización si la descripción ya existe. Es el mismo endpoint
     * que usa el formulario de dictamen para registrar equipos sobre la marcha.
     */
    create: (data) => api.post('/v1/equipos/upsert', data),

    /**
     * Edición de una entrada concreta.
     *
     * La pantalla anterior llamaba a `/api/v1/equipos/{id}` con el `baseURL`
     * que ya incluye `/api`, así que la URL final era `/api/api/v1/…`: editar
     * un equipo devolvía siempre 404.
     */
    update: (id, data) => api.put(`/v1/equipos/${id}`, data),

    delete: (id) => api.delete(`/v1/equipos/${id}`),
};

export default equipoService;
