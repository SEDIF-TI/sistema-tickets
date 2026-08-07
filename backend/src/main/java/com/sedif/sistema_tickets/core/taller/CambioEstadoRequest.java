package com.sedif.sistema_tickets.core.taller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Cambio de estado de un equipo dentro del taller.
 *
 * <p>El patron admite exactamente los valores de
 * {@code util.enums.EstadoTaller}, sin distinguir mayusculas. Acotarlo aqui
 * hace que un estado mal escrito se rechace con un 400 antes de llegar al
 * servicio, en vez de guardarse y dejar el equipo en una situacion inexistente
 * que ningun filtro del listado encontraria.</p>
 *
 * <p>El servicio vuelve a resolver el texto contra el enum: esta validacion
 * cubre la forma del dato, no sustituye a la conversion.</p>
 */
public record CambioEstadoRequest(

        @NotBlank(message = "Debe indicar el nuevo estado del equipo.")
        @Pattern(
                regexp = "(?i)RECIBIDO|EN_DIAGNOSTICO|EN_REPARACION|REPARADO|ENTREGADO|IRREPARABLE",
                message = "Estado no valido. Valores admitidos: RECIBIDO, EN_DIAGNOSTICO, "
                        + "EN_REPARACION, REPARADO, ENTREGADO, IRREPARABLE.")
        String estado

) {}
