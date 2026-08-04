import api from './api';

/**
 * Resguardos de equipo: préstamos temporales que el área de TI entrega al
 * personal. Espeja ResguardoResource del backend.
 *
 * Exige rol SOPORTE o ADMINISTRADOR.
 */
export const resguardoService = {
    /**
     * Listado paginado, con búsqueda y filtro de estado resueltos en la base.
     * Antes el filtrado ocurría sobre la página ya descargada, así que buscar
     * un número de serie solo miraba los diez resguardos visibles.
     */
    getAll: (params = {}) => api.get('/v1/resguardos', { params }),

    create: (data) => api.post('/v1/resguardos', data),

    /** Marca el equipo como devuelto y cierra el resguardo. */
    devolver: (id) => api.put(`/v1/resguardos/${id}/devolucion`),
};

export default resguardoService;
