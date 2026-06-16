import api from './api'; // <-- ¡ESTA ES LA LÍNEA CLAVE QUE FALTABA!

export const userService = {
    getAll: () => api.get('/v1/admin/usuarios'),
    create: (data) => api.post('/v1/admin/usuarios', data),
    update: (id, data) => api.put(`/v1/admin/usuarios/${id}`, data),
    resetPassword: (id) => api.post(`/v1/admin/usuarios/${id}/reset-password`)
};