package com.sedif.sistema_tickets.core.taller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada del taller de reparaciones.
 *
 * <p>Sustituye al {@code @RequestBody EquipoReparacion} que recibia antes el
 * controlador. Aceptar la entidad JPA directamente es un problema de
 * <i>mass assignment</i>: el cliente podia enviar campos que no le
 * corresponden —{@code id}, {@code folio}, {@code tecnicoAsignado}, las
 * columnas de auditoria— y sobrescribir registros ajenos o falsear quien
 * atendio una reparacion.</p>
 *
 * <p>El {@code folio} no esta aqui a proposito: lo genera el servidor. El
 * {@code estadoTaller} tampoco: toda alta entra como RECIBIDO y los cambios
 * pasan por el endpoint de estado.</p>
 */
public record EquipoReparacionRequest(

        // --- Solicitante ---
        @NotBlank(message = "El nombre del solicitante es obligatorio.")
        @Size(max = 200, message = "El nombre no puede exceder 200 caracteres.")
        String solicitanteNombre,

        @Size(max = 50, message = "El numero del solicitante no puede exceder 50 caracteres.")
        String solicitanteNumero,

        @Size(max = 150, message = "El departamento no puede exceder 150 caracteres.")
        String departamento,

        // --- Equipo ---
        @NotBlank(message = "Debe indicar el tipo de equipo.")
        @Size(max = 100, message = "El tipo de equipo no puede exceder 100 caracteres.")
        String equipoTipo,

        @Size(max = 100, message = "La marca no puede exceder 100 caracteres.")
        String marca,

        @Size(max = 100, message = "El modelo no puede exceder 100 caracteres.")
        String modelo,

        @Size(max = 100, message = "El numero de serie no puede exceder 100 caracteres.")
        String numeroSerie,

        @Size(max = 100, message = "El numero de inventario no puede exceder 100 caracteres.")
        String numeroInventario,

        // --- Recepcion y diagnostico ---
        @Size(max = 500, message = "La condicion de recepcion no puede exceder 500 caracteres.")
        String condicionRecepcion,

        @Size(max = 500, message = "Los accesorios no pueden exceder 500 caracteres.")
        String accesorios,

        @NotBlank(message = "Debe describir la falla reportada.")
        @Size(max = 1000, message = "La falla reportada no puede exceder 1000 caracteres.")
        String fallaReportada,

        @Size(max = 1000, message = "El diagnostico no puede exceder 1000 caracteres.")
        String diagnostico,

        @Size(max = 1000, message = "La solucion no puede exceder 1000 caracteres.")
        String solucion,

        /** Ticket de origen, cuando la reparacion nace de un reporte. */
        Long ticketId

) {}
