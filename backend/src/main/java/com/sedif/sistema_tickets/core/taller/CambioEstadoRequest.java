package com.sedif.sistema_tickets.core.taller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Cambio de estado de un equipo dentro del taller.
 *
 * <p>Sustituye al {@code Map<String, String>} que recibia antes el endpoint:
 * aceptaba cualquier estructura y cualquier texto como estado, de modo que un
 * error de escritura dejaba el equipo en un estado inexistente, invisible para
 * los filtros del listado.</p>
 *
 * <p>El patron admite exactamente los valores de
 * {@code util.enums.EstadoTaller}, sin distinguir mayusculas.</p>
 */
public record CambioEstadoRequest(

        @NotBlank(message = "Debe indicar el nuevo estado del equipo.")
        @Pattern(
                regexp = "(?i)RECIBIDO|EN_DIAGNOSTICO|EN_REPARACION|REPARADO|ENTREGADO|IRREPARABLE",
                message = "Estado no valido. Valores admitidos: RECIBIDO, EN_DIAGNOSTICO, "
                        + "EN_REPARACION, REPARADO, ENTREGADO, IRREPARABLE.")
        String estado

) {}
