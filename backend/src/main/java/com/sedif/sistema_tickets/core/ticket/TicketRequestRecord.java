package com.sedif.sistema_tickets.core.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para levantar un ticket de soporte.
 *
 * <p>Los limites de longitud se corresponden con las columnas de la tabla
 * {@code ticket}: si el texto excede el tamano, es preferible un 400 con un
 * mensaje claro que un 500 por truncamiento en la base de datos.</p>
 */
public record TicketRequestRecord(

        @NotBlank(message = "El titulo de la falla es obligatorio.")
        @Size(max = 255, message = "El titulo no puede exceder 255 caracteres.")
        String titulo,

        @Size(max = 255, message = "La sede no puede exceder 255 caracteres.")
        String sede,

        @NotBlank(message = "La descripcion de la falla es obligatoria.")
        @Size(max = 5000, message = "La descripcion no puede exceder 5000 caracteres.")
        String descripcion,

        @Size(max = 20, message = "La prioridad no puede exceder 20 caracteres.")
        String prioridad,

        /** Persona afectada, cuando no coincide con quien levanta el ticket. */
        @Size(max = 100, message = "El nombre del solicitante no puede exceder 100 caracteres.")
        String solicitante,

        /**
         * Asignacion manual a un tecnico. Solo la usa el administrador; si
         * viene vacio, el balanceador elige al tecnico con menos carga.
         */
        Long usuarioSoporteId

) {}
