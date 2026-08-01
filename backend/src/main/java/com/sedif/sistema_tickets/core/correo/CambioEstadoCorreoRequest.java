package com.sedif.sistema_tickets.core.correo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Cambio de situacion de una cuenta de correo institucional.
 *
 * <p>Sustituye al {@code Map<String, String>} sin tipo que recibia antes el
 * endpoint, que aceptaba cualquier texto como estado.</p>
 *
 * <p>Los valores admitidos son los de {@code util.enums.EstadoCorreo}.</p>
 */
public record CambioEstadoCorreoRequest(

        @NotBlank(message = "Debe indicar el nuevo estado de la cuenta.")
        @Pattern(
                regexp = "(?i)ACTIVO|BAJA|SUSPENDIDO",
                message = "Estado no valido. Valores admitidos: ACTIVO, BAJA, SUSPENDIDO.")
        String estado

) {}
