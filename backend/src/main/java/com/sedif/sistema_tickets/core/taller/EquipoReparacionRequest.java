package com.sedif.sistema_tickets.core.taller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada del taller de reparaciones.
 *
 * <p>El controlador recibe este record y no la entidad JPA para cerrar la via
 * de <i>mass assignment</i>: si el cuerpo se vinculara a la entidad, el cliente
 * podria enviar {@code id}, {@code folio}, {@code tecnicoAsignado} o las
 * columnas de auditoria y con ello sobrescribir registros ajenos o falsear
 * quien atendio una reparacion.</p>
 *
 * <p>Por eso quedan fuera los campos que no le corresponden decidir: el
 * {@code folio} lo genera el servidor, y el {@code estadoTaller} arranca en
 * RECIBIDO y solo avanza por el endpoint de cambio de estado, que valida el
 * valor contra el enum.</p>
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
