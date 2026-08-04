package com.sedif.sistema_tickets.core.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Respuesta del solicitante a la encuesta de satisfaccion.
 *
 * <p>El comentario es opcional a proposito: exigirlo haria que la gente
 * escribiera cualquier cosa por salir del paso, y la calificacion perderia
 * valor. Quien quiera explicar el porque, tiene sitio para hacerlo.</p>
 */
public record EncuestaRequest(

        @NotBlank(message = "Debe indicar como fue el servicio recibido.")
        @Pattern(regexp = "(?i)MALO|REGULAR|BUENO",
                 message = "La calificacion debe ser MALO, REGULAR o BUENO.")
        String calificacion,

        @Size(max = 500, message = "El comentario no puede exceder 500 caracteres.")
        String comentario

) {}
