package com.sedif.sistema_tickets.core.taller;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Registro de taller tal como lo consume el frontend.
 *
 * <p>Se envia en lugar de la entidad para no exponer el modelo JPA ni sus
 * relaciones perezosas: el tecnico asignado viaja aplanado en dos campos
 * ({@code tecnicoAsignadoId} y {@code tecnicoAsignadoNombre}), resueltos
 * mientras la sesion sigue abierta.</p>
 */
@Data
public class EquipoReparacionDTO {
    private Long id;
    private String folio;
    private Long ticketId;

    // Solicitante
    private String solicitanteNombre;
    private String solicitanteNumero;
    private String departamento;
    
    // Identificacion del equipo
    private String equipoTipo;
    private String marca;
    private String modelo;
    private String numeroSerie;
    private String numeroInventario;
    
    // Recepcion y avance de la reparacion
    private String condicionRecepcion;
    private String accesorios;
    private String fallaReportada;
    private String diagnostico;
    private String solucion;
    private String estadoTaller;
    
    // Tecnico asignado, aplanado: evita anidar el usuario completo en la
    // respuesta y con el la relacion perezosa de la entidad.
    private Long tecnicoAsignadoId;
    private String tecnicoAsignadoNombre;

    // Fecha de alta heredada de Auditable, para ordenar el historial.
    private LocalDateTime fechaCreacion;
}