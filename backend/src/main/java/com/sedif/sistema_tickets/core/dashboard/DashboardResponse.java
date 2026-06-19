package com.sedif.sistema_tickets.core.dashboard;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class DashboardResponse {
    private long totalTickets;
    private long totalUsuarios;
    private long totalBitacora;
    
    private List<MetricaGenerica> porEstatus;
    private List<MetricaGenerica> porArea; // "Departamentos" en tu imagen
    private List<MetricaGenerica> porPrioridad; // Usaremos esto para "Categorías" o Prioridad
    private List<MetricaGenerica> porIngeniero;
    private List<MetricaFecha> porFecha;

    @Data
    @Builder
    public static class MetricaGenerica {
        private String nombre;
        private long cantidad;
    }

    @Data
    @Builder
    public static class MetricaFecha {
        private String fecha;
        private long total;
        private long resueltos;
    }
}