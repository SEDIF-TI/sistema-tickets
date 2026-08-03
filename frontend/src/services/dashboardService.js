import api from './api';

/**
 * Métricas del panel de administración.
 * Espeja DashboardResource del backend (solo rol ADMINISTRADOR).
 */
export const dashboardService = {
    getMetricas: () => api.get('/v1/admin/dashboard/metricas')
};
