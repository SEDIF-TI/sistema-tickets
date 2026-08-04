package com.sedif.sistema_tickets.core.area;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de alta y edicion de un area.
 *
 * <p>Antes no validaba nada: un cuerpo vacio creaba un area sin nombre, que
 * aparecia como una fila en blanco en todos los selectores del sistema y no
 * habia forma de distinguirla ni de corregirla desde la interfaz.</p>
 */
public record AreaRecord(

        @NotBlank(message = "El nombre del area es obligatorio.")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres.")
        String nombre,

        Boolean activo,

        /**
         * Un area prioritaria recibe atencion preferente. Al crear, si no se
         * indica, queda en {@code false}.
         */
        Boolean prioritaria

) {}
