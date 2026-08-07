import api from './api';

/**
 * Resguardos de equipo: préstamos temporales que el área de TI entrega al
 * personal. Espeja ResguardoResource del backend.
 *
 * Exige rol SOPORTE o ADMINISTRADOR.
 */
export const resguardoService = {
    /**
     * Listado paginado. La búsqueda y el filtro de estado viajan como
     * parámetros y se resuelven en la base, de modo que alcanzan a todos los
     * resguardos y no solo a la página que el navegador tiene descargada.
     */
    getAll: (params = {}) => api.get('/v1/resguardos', { params }),

    create: (data) => api.post('/v1/resguardos', data),

    /** Marca el equipo como devuelto y cierra el resguardo. */
    devolver: (id) => api.put(`/v1/resguardos/${id}/devolucion`),
};

export default resguardoService;
