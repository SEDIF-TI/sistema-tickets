package com.sedif.sistema_tickets.core.resguardo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar la entrega de un equipo en resguardo.
 *
 * <p>Los limites coinciden con las columnas de la tabla {@code resguardo}: es
 * preferible un 400 con mensaje claro que un 500 por truncamiento en la base
 * de datos.</p>
 */
public record ResguardoRequest(

        @NotBlank(message = "El nombre del solicitante es obligatorio.")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres.")
        String solicitanteNombre,

        @NotBlank(message = "El numero de empleado del solicitante es obligatorio.")
        @Size(max = 30, message = "El numero de empleado no puede exceder 30 caracteres.")
        String solicitanteNumero,

        @NotBlank(message = "Debe indicar que equipo se entrega.")
        @Size(max = 100, message = "El nombre del equipo no puede exceder 100 caracteres.")
        String equipoNombre,

        @NotBlank(message = "El numero de serie es obligatorio.")
        @Size(max = 50, message = "El numero de serie no puede exceder 50 caracteres.")
        String numeroSerie,

        @Size(max = 500, message = "Los accesorios no pueden exceder 500 caracteres.")
        String accesorios,

        /**
         * Duracion del prestamo. Junto con duracionTipo determina la fecha de
         * vencimiento; si se omite, el resguardo queda sin plazo.
         */
        @Min(value = 1, message = "La duracion debe ser de al menos 1.")
        @Max(value = 365, message = "La duracion no puede superar 365 unidades.")
        Integer duracionCantidad,

        @Pattern(regexp = "(?i)dias|semanas|meses|indefinido",
                 message = "La duracion debe expresarse en dias, semanas, meses o ser indefinida.")
        String duracionTipo,

        @Size(max = 50, message = "El telefono no puede exceder 50 caracteres.")
        String telefono,

        @Size(max = 100, message = "El departamento no puede exceder 100 caracteres.")
        String departamento,

        @Size(max = 50, message = "El numero de inventario no puede exceder 50 caracteres.")
        String numeroInventario,

        @Size(max = 500, message = "Las condiciones no pueden exceder 500 caracteres.")
        String condiciones,

        /** Nombre de quien hace la entrega, para el formato impreso. */
        @Size(max = 100, message = "El nombre de quien entrega no puede exceder 100 caracteres.")
        String entregaNombre

) {}
