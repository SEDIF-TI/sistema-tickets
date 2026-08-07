package com.sedif.sistema_tickets.core.area;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de alta y edicion de un area.
 *
 * <p>El nombre es obligatorio: identifica al area en todos los selectores del
 * sistema, de modo que un area sin nombre seria indistinguible del resto.</p>
 */
public record AreaRecord(

        @NotBlank(message = "El nombre del area es obligatorio.")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres.")
        String nombre,

        Boolean activo,

        /**
         * Un area prioritaria recibe atencion preferente. Si no se indica en el
         * alta, queda en {@code false}.
         */
        Boolean prioritaria

) {}
