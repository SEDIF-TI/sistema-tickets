import api from './api';

/**
 * Operaciones sobre tickets. Espeja TicketResource del backend.
 *
 * Los listados reciben el objeto de parámetros que arma `useTablaPaginada`
 * ({ page, size, sort, busqueda, ...filtros }) y devuelven un `PageResponse`:
 * { contenido, pagina, tamano, totalPaginas, totalItems, esPrimera, esUltima }.
 *
 * `atender` y `resolver` solo están disponibles para SOPORTE y ADMINISTRADOR:
 * si los invoca un empleado, el backend responde 403.
 */
export const ticketService = {
    /** Bandeja del usuario; el backend filtra según su rol y su área. */
    getAll: (params = {}) => api.get('/v1/tickets', { params }),

    /** Historial del área del usuario autenticado. */
    getMisTickets: (params = {}) => api.get('/v1/tickets/mis-tickets', { params }),

    create: (data) => api.post('/v1/tickets', data),

    /** Cierre por parte de quien solicitó el ticket. */
    finalizar: (id) => api.put(`/v1/tickets/${id}/finalizar`),

    /** Toma del ticket por un técnico. */
    atender: (id) => api.put(`/v1/tickets/${id}/atender`),

    /**
     * Resolución con justificación obligatoria.
     *
     * `planTrabajoClave` es opcional: no toda resolución se imputa a una meta
     * del plan anual de trabajo.
     */
    resolver: (id, justificacion, planTrabajoClave = null) =>
        api.put(`/v1/tickets/${id}/resolver`, { justificacion, planTrabajoClave }),
};

export default ticketService;
