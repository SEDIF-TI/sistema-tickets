import api from './api';

const RUTA = '/taller';

/**
 * Equipos recibidos en el taller. Espeja EquipoReparacionResource.
 *
 * Exige rol SOPORTE o ADMINISTRADOR.
 *
 * La versión anterior envolvía cada error en `new Error(...)` leyendo
 * `error.response.data.mensaje`, campo que el backend no usa (envía `message`).
 * El resultado era que todos los fallos mostraban el texto genérico de
 * respaldo, ocultando el motivo real. Ahora los errores se propagan tal cual y
 * los traduce el interceptor de api.js, que ya deja el mensaje en
 * `error.mensaje`.
 */
export const tallerService = {
    /** Listado paginado, con búsqueda y filtro de estado resueltos en la base. */
    getAll: (params = {}) => api.get(RUTA, { params }),

    /** Catálogo de estados del ciclo de vida, servido desde el enum. */
    getEstados: () => api.get(`${RUTA}/estados`),

    getById: (id) => api.get(`${RUTA}/${id}`),

    /** Registra la entrada de un equipo. El folio lo genera el backend. */
    create: (data, tecnicoId = null) =>
        api.post(RUTA, data, { params: tecnicoId ? { tecnicoId } : {} }),

    update: (id, data, tecnicoId = null) =>
        api.put(`${RUTA}/${id}`, data, { params: tecnicoId ? { tecnicoId } : {} }),

    /** Avanza el equipo en su ciclo de vida. */
    cambiarEstado: (id, estado) => api.patch(`${RUTA}/${id}/estado`, { estado }),
};

export default tallerService;
