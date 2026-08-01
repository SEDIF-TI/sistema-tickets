package com.sedif.sistema_tickets.core.actividad;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Registro de una actividad del plan anual de trabajo.
 *
 * <p>Sustituye al {@code Map<String, Object>} sin tipo que recibia el
 * endpoint. Aquel enfoque tenia dos problemas serios:</p>
 * <ul>
 *   <li>Aceptaba un {@code usuarioId} enviado por el cliente, de modo que
 *       cualquiera podia registrar actividades <b>a nombre de otra persona</b>.
 *       Ahora la identidad se toma del token y ese campo desaparece.</li>
 *   <li>Llamaba a {@code payload.get("...").toString()} sin comprobar nulos:
 *       omitir un campo provocaba un {@code NullPointerException} y una
 *       respuesta 500 en lugar de un mensaje de validacion.</li>
 * </ul>
 */
public record ActividadExtraRequest(

        @NotNull(message = "Debe indicar la meta del plan de trabajo.")
        Integer planTrabajoClave,

        @NotBlank(message = "La actividad solicitada es obligatoria.")
        @Size(max = 255, message = "La actividad no puede exceder 255 caracteres.")
        String actividadSolicitada,

        @NotBlank(message = "La situacion actual es obligatoria.")
        @Size(max = 50, message = "La situacion actual no puede exceder 50 caracteres.")
        String situacionActual,

        @Size(max = 2000, message = "La justificacion no puede exceder 2000 caracteres.")
        String justificacion

) {}
