package com.sedif.sistema_tickets.core.documento;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Periodo y alcance del reporte de actividades, en sus dos formatos.
 *
 * <p>Las fechas se deserializan como {@link LocalDate} en formato ISO, de modo
 * que una fecha mal escrita produce un 400 con el detalle del campo en lugar de
 * un fallo al convertirla.</p>
 *
 * <p>El rol y el identificador de usuario acotan el alcance: un ADMINISTRADOR
 * obtiene el reporte de toda la institucion y cualquier otro rol solo el de sus
 * propios tickets.</p>
 */
public record ReporteActividadesRequest(

        @NotNull(message = "La fecha de inicio es obligatoria.")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate fechaInicio,

        @NotNull(message = "La fecha final es obligatoria.")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate fechaFin,

        /** Rol de quien pide el reporte. Un valor desconocido se trata como no administrador. */
        String rol,

        /** Tecnico del que se reportan las actividades cuando el rol no es ADMINISTRADOR. */
        Long usuarioId

) {
    /**
     * Comprueba que el periodo tenga sentido.
     *
     * <p>Se valida aqui y no con una anotacion porque la regla cruza dos campos:
     * Bean Validation solo alcanza a uno cada vez.</p>
     *
     * @throws IllegalArgumentException si la fecha inicial es posterior a la final.
     */
    public void validarPeriodo() {
        if (fechaInicio != null && fechaFin != null && fechaInicio.isAfter(fechaFin)) {
            throw new IllegalArgumentException(
                    "La fecha de inicio no puede ser posterior a la fecha final.");
        }
    }
}
