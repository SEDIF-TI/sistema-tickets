import api from './api';

export const userService = {
    // Agregamos el /v1 a todas las rutas para que hagan match con Java
    getAll: () => api.get('/v1/admin/usuarios'),
    
    // Métodos necesarios para el Modal de creación y edición
    create: (data) => api.post('/v1/admin/usuarios', data),
    update: (id, data) => api.put(`/v1/admin/usuarios/${id}`, data),
    
    resetPassword: (id) => api.put(`/v1/admin/usuarios/${id}/reset-password`),
    toggleEstado: (id) => api.put(`/v1/admin/usuarios/${id}/toggle-estado`)
};