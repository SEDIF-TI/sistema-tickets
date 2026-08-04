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

    /** Reparto global de calificaciones: cuantos buenos, regulares y malos. */
    private List<MetricaGenerica> porCalificacion;

    /** Calificacion media de cada tecnico, con el numero de respuestas. */
    private List<MetricaCalificacion> calificacionPorTecnico;

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

    /**
     * Calificacion de un tecnico.
     *
     * <p>Lleva el numero de respuestas junto a la media a proposito: sin el, un
     * 3.0 logrado con una sola encuesta pareceria mejor que un 2.8 sostenido
     * sobre cuarenta.</p>
     */
    @Data
    @Builder
    public static class MetricaCalificacion {
        private String nombre;
        private Double promedio;
        private Long respuestas;
    }
}
