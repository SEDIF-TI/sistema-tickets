import api from './api';

/**
 * Avisos del sistema. Espeja AvisoResource del backend.
 *
 * Solo `getActivos` está abierto a cualquier usuario autenticado: es el que
 * alimenta la barra de avisos de todos los paneles. El resto de operaciones
 * exige rol ADMINISTRADOR, porque un aviso es visible para toda la
 * institución.
 */
export const avisoService = {
    /** Avisos vigentes para el panel del usuario. */
    getActivos: () => api.get('/v1/avisos/activos'),

    /** Listado completo, incluidos inactivos. Solo administración. */
    getAll: () => api.get('/v1/avisos'),

    create: (data) => api.post('/v1/avisos', data),
    update: (id, data) => api.put(`/v1/avisos/${id}`, data),
    delete: (id) => api.delete(`/v1/avisos/${id}`)
};
