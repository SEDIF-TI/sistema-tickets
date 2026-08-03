package com.sedif.sistema_tickets.core.usuarios;

import jakarta.validation.constraints.NotNull;

/**
 * Cambio de disponibilidad de un tecnico de soporte.
 *
 * <p>{@code @NotNull} evita que un cuerpo vacio o mal formado llegue al
 * servicio y provoque un {@code NullPointerException} al desempaquetar el
 * Boolean.</p>
 */
public record DisponibilidadRequest(

        @NotNull(message = "Debe indicar la disponibilidad (true o false).")
        Boolean disponible

) {}
