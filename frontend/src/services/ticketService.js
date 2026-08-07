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
    /**
     * Bandeja del usuario; el backend filtra según su rol y su área.
     * Admite `busqueda` y `estatus`, que se resuelven en la base de datos.
     */
    getAll: (params = {}) => api.get('/v1/tickets', { params }),

    /** Historial del área del usuario autenticado. */
    getMisTickets: (params = {}) => api.get('/v1/tickets/mis-tickets', { params }),

    /**
     * Catálogo de metas del plan anual de trabajo, para el desplegable de
     * resolución. Se sirve desde el enum `PlanTrabajo`, que es la misma fuente
     * contra la que el backend valida la clave al cerrar el ticket.
     */
    getPlanTrabajo: () => api.get('/v1/tickets/plan-trabajo'),

    /**
     * Catálogo de prioridades para el formulario de alta. Se sirve desde el
     * enum `Prioridad` del backend, de modo que pantalla y validación del
     * servidor no puedan discrepar.
     */
    getPrioridades: () => api.get('/v1/tickets/prioridades'),

    /** Catálogo de calificaciones de la encuesta de satisfacción. */
    getCalificaciones: () => api.get('/v1/tickets/calificaciones'),

    /**
     * Registra la encuesta del solicitante.
     *
     * Solo puede calificar quien levantó el ticket, una sola vez y con el
     * ticket ya cerrado: el backend lo comprueba, porque depende del ticket
     * concreto y no del rol.
     */
    calificar: (id, calificacion, comentario = null) =>
        api.put(`/v1/tickets/${id}/calificar`, { calificacion, comentario }),

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
