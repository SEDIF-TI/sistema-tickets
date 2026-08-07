package com.sedif.sistema_tickets.core.equipo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de alta y edicion de un equipo del catalogo.
 *
 * <p>Los endpoints reciben este record y no la entidad {@link Equipo}: al no
 * exponer el {@code id}, el cuerpo de la peticion no puede apuntar a una fila
 * existente para sobrescribirla. Las anotaciones de validacion rechazan una
 * descripcion vacia en la capa web, antes de que la restriccion NOT NULL de la
 * base de datos la convierta en un error sin explicacion.</p>
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
