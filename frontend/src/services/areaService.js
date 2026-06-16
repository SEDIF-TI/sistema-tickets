import api from './api';

export const areaService = {
    getAll: () => api.get('/v1/admin/areas'),
    create: (data) => api.post('/v1/admin/areas', data),
    update: (id, data) => api.put(`/v1/admin/areas/${id}`, data),
    delete: (id) => api.delete(`/v1/admin/areas/${id}`),
    
    // <-- NUEVO MÉTODO PARA EL PATCH -->
    asignarSoporteFijo: (id, soporteFijoId) => api.patch(`/v1/admin/areas/${id}/soporte-fijo`, { soporteFijoId })
};