package com.sedif.sistema_tickets.core.aviso;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de alta y edicion de un aviso.
 *
 * <p>Las anotaciones de validacion cubren por igual el alta y la edicion: el
 * titulo y el mensaje se dibujan en la barra de todos los paneles, asi que
 * ninguno de los dos puede quedar vacio.</p>
 */
public record AvisoRequestRecord(

        @NotBlank(message = "El titulo del aviso es obligatorio.")
        @Size(max = 150, message = "El titulo no puede exceder 150 caracteres.")
        String titulo,

        @NotBlank(message = "El mensaje del aviso es obligatorio.")
        @Size(max = 2000, message = "El mensaje no puede exceder 2000 caracteres.")
        String mensaje,

        Boolean activo,

        /** Sin area, el aviso es global y lo ve toda la institucion. */
        Long areaId

) {}
