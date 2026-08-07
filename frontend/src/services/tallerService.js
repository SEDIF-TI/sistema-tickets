import api from './api';

const RUTA = '/taller';

/**
 * Equipos recibidos en el taller. Espeja EquipoReparacionResource.
 *
 * Exige rol SOPORTE o ADMINISTRADOR.
 *
 * Los errores se propagan sin envolver: el interceptor de `api.js` ya deja el
 * texto del backend en `error.mensaje`, que es de donde lo toma
 * `notificarError`. Envolverlos aquí sustituiría el motivo real por un mensaje
 * genérico.
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
