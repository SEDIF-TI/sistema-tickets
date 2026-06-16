// src/services/userService.js
import api from './api';

export const userService = {
    getAll: () => api.get('/admin/usuarios'),
    resetPassword: (id) => api.put(`/admin/usuarios/${id}/reset-password`),
    toggleEstado: (id) => api.put(`/admin/usuarios/${id}/toggle-estado`),
};