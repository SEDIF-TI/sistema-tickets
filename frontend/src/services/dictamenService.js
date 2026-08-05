import api from './api';

/**
 * Dictámenes técnicos emitidos por el área. Espeja DictamenResource.
 *
 * La emisión no vive aquí: el dictamen se crea al generar su PDF desde
 * `/v1/documentos/dictamen`, que además lo registra. Este servicio solo
 * consulta lo ya emitido.
 *
 * Los dictámenes anteriores a la migración V10 no aparecen: hasta entonces el
 * sistema componía el PDF y lo devolvía sin guardar nada.
 *
 * Exige rol SOPORTE o ADMINISTRADOR.
 */
export const dictamenService = {
    /** Listado paginado, con búsqueda resuelta en la base. */
    getAll: (params = {}) => api.get('/v1/dictamenes', { params }),

    getById: (id) => api.get(`/v1/dictamenes/${id}`),
};

export default dictamenService;
