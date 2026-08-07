package com.sedif.sistema_tickets.core.dashboard;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * Conjunto de metricas que alimenta el panel de administracion.
 *
 * <p>Reune en una sola respuesta los totales de cabecera y una lista por cada
 * grafica, de modo que la pantalla se dibuje con una unica llamada.</p>
 */
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
    
    /** Avisos activos agrupados por area. */
    private List<MetricaGenerica> avisosPorArea;

    /** Reparto global de calificaciones: cuantos buenos, regulares y malos. */
    private List<MetricaGenerica> porCalificacion;

    /** Calificacion media de cada tecnico, con el numero de respuestas. */
    private List<MetricaCalificacion> calificacionPorTecnico;

    /** Par etiqueta y conteo, la forma que consumen casi todas las graficas. */
    @Data
    @Builder
    public static class MetricaGenerica {
        private String nombre;
        private long cantidad;
    }

    /** Punto de la serie diaria: cuantos tickets entraron y cuantos se resolvieron. */
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
     *
     * <p>La media puede ser nula cuando el tecnico aun no ha recibido ninguna
     * calificacion.</p>
     */
    @Data
    @Builder
    public static class MetricaCalificacion {
        private String nombre;
        private Double promedio;
        private Long respuestas;
    }
}
