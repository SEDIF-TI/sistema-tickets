package com.sedif.sistema_tickets.core.taller;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EquipoReparacionDTO {
    private Long id;
    private String folio;
    private Long ticketId;
    
    // Datos del Solicitante
    private String solicitanteNombre;
    private String solicitanteNumero;
    private String departamento;
    
    // Datos del Equipo
    private String equipoTipo;
    private String marca;
    private String modelo;
    private String numeroSerie;
    private String numeroInventario;
    
    // Detalles del Taller
    private String condicionRecepcion;
    private String accesorios;
    private String fallaReportada;
    private String diagnostico;
    private String solucion;
    private String estadoTaller;
    
    // Datos del Técnico Asignado (Aplanados para React)
    private Long tecnicoAsignadoId;
    private String tecnicoAsignadoNombre;

    // Fechas de auditoría (si heredaste de Auditable, las puedes mapear aquí)
    private LocalDateTime fechaCreacion;
}