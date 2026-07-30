import api from './api';

const API_URL = '/taller';

export const tallerService = {
  // Obtener equipos registrados en el taller con filtro opcional
  obtenerEquipos: async (filtro = '') => {
    try {
      const params = new URLSearchParams();
      if (filtro) params.append('filtro', filtro);

      const response = await api.get(`${API_URL}?${params.toString()}`);
      return response.data;
    } catch (error) {
      throw new Error(error.response?.data?.mensaje || 'Error al obtener los equipos del taller');
    }
  },

  // Obtener detalle de un registro por ID
  obtenerPorId: async (id) => {
    try {
      const response = await api.get(`${API_URL}/${id}`);
      return response.data;
    } catch (error) {
      throw new Error(error.response?.data?.mensaje || 'Error al consultar el equipo');
    }
  },

  // Registrar nuevo equipo en taller
  registrarEquipo: async (equipoData, tecnicoId = null) => {
    try {
      const params = new URLSearchParams();
      if (tecnicoId) params.append('tecnicoId', tecnicoId);

      const url = `${API_URL}${params.toString() ? '?' + params.toString() : ''}`;
      const response = await api.post(url, equipoData);
      return response.data;
    } catch (error) {
      const msg = error.response?.data?.mensaje || 'Error al registrar la entrada del equipo';
      throw new Error(msg);
    }
  },

  // Actualizar equipo/diagnóstico existente
  actualizarEquipo: async (id, equipoData, tecnicoId = null) => {
    try {
      const params = new URLSearchParams();
      if (tecnicoId) params.append('tecnicoId', tecnicoId);

      const url = `${API_URL}/${id}${params.toString() ? '?' + params.toString() : ''}`;
      const response = await api.put(url, equipoData);
      return response.data;
    } catch (error) {
      throw new Error(error.response?.data?.mensaje || 'Error al actualizar el registro');
    }
  },

  // Cambiar el estado del taller rápidamente
  cambiarEstado: async (id, nuevoEstado) => {
    try {
      const response = await api.patch(`${API_URL}/${id}/estado`, { estado: nuevoEstado });
      return response.data;
    } catch (error) {
      throw new Error(error.response?.data?.mensaje || 'Error al cambiar el estado del equipo');
    }
  }
};