// Importa tu instancia de Axios que ya inyecta el token automáticamente
import api from './api'; 

const API_URL = '/correos';

export const correoService = {
  // Obtener correos con filtros opcionales
  obtenerCorreos: async (filtro = '', estado = '') => {
    try {
      const params = new URLSearchParams();
      if (filtro) params.append('filtro', filtro);
      if (estado) params.append('estado', estado);

      const response = await api.get(`${API_URL}?${params.toString()}`);
      return response.data;
    } catch (error) {
      throw new Error(error.response?.data?.mensaje || 'Error al obtener los correos');
    }
  },

  // Crear un nuevo registro
  crearCorreo: async (correoData) => {
    try {
      const response = await api.post(API_URL, correoData);
      return response.data;
    } catch (error) {
      const msg = error.response?.data?.mensaje || error.response?.data?.message || 'Error al registrar el correo';
      throw new Error(msg);
    }
  },

  // Actualizar un registro existente
  actualizarCorreo: async (id, correoData) => {
    try {
      const response = await api.put(`${API_URL}/${id}`, correoData);
      return response.data;
    } catch (error) {
      throw new Error(error.response?.data?.mensaje || 'Error al actualizar el correo');
    }
  },

  // Cambiar estado
  cambiarEstado: async (id, nuevoEstado) => {
    try {
      const response = await api.patch(`${API_URL}/${id}/estado`, { estado: nuevoEstado });
      return response.data;
    } catch (error) {
      throw new Error(error.response?.data?.mensaje || 'Error al cambiar estado');
    }
  },

  // Eliminar registro
  eliminarCorreo: async (id) => {
    try {
      const response = await api.delete(`${API_URL}/${id}`);
      return response.data;
    } catch (error) {
      throw new Error(error.response?.data?.mensaje || 'Error al eliminar');
    }
  },
};