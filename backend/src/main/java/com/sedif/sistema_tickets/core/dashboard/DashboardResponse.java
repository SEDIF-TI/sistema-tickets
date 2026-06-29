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
    private List<MetricaGenerica> porArea; 
    private List<MetricaGenerica> porPrioridad; 
    private List<MetricaGenerica> porIngeniero;
    private List<MetricaFecha> porFecha;
    
    // ---> NUEVO: Lista para alimentar la nueva gráfica de avisos
    private List<MetricaGenerica> avisosPorArea; 

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