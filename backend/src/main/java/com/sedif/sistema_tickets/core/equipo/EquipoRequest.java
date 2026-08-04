package com.sedif.sistema_tickets.core.equipo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de alta y edicion de un equipo del catalogo.
 *
 * <p>Los endpoints recibian la entidad {@link Equipo} directamente. Eso
 * permitia enviar un {@code id} en el cuerpo y, en el alta, apuntar a una fila
 * existente para sobrescribirla (mass assignment); tampoco habia ninguna
 * anotacion de validacion pese al {@code @Valid} de los controladores, asi que
 * una descripcion vacia solo fallaba al llegar a la restriccion NOT NULL de la
 * base de datos, con un 500 sin explicacion.</p>
 */
public record EquipoRequest(

        @NotBlank(message = "La descripcion del equipo es obligatoria.")
        @Size(max = 255, message = "La descripcion no puede exceder 255 caracteres.")
        String descripcion,

        @Size(max = 255, message = "La marca no puede exceder 255 caracteres.")
        String marca,

        @Size(max = 255, message = "El modelo no puede exceder 255 caracteres.")
        String modelo

) {}
