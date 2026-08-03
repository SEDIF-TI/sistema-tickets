import api from './api';

/**
 * Operaciones sobre tickets. Espeja TicketResource del backend.
 *
 * `atender` y `resolver` solo están disponibles para SOPORTE y ADMINISTRADOR:
 * si los invoca un empleado, el backend responde 403.
 */
export const ticketService = {
    /**
     * Bandeja del usuario, paginada. El backend decide qué tickets son
     * visibles según el nivel de visión del rol y devuelve
     * { contenido, pagina, tamano, totalPaginas, totalItems, ... }.
     */
    getAll: (pagina = 0, tamano = 10) =>
        api.get('/v1/tickets', { params: { page: pagina, size: tamano } }),

    /** Historial del área del usuario autenticado, paginado. */
    getMisTickets: (pagina = 0, tamano = 10) =>
        api.get('/v1/tickets/mis-tickets', { params: { page: pagina, size: tamano } }),

    create: (data) => api.post('/v1/tickets', data),

    /** Cierre por parte de quien solicitó el ticket. */
    finalizar: (id) => api.put(`/v1/tickets/${id}/finalizar`),

    /** Toma del ticket por un técnico. */
    atender: (id) => api.put(`/v1/tickets/${id}/atender`),

    /** Resolución con justificación obligatoria. */
    resolver: (id, justificacion) =>
        api.put(`/v1/tickets/${id}/resolver`, { justificacion })
};
