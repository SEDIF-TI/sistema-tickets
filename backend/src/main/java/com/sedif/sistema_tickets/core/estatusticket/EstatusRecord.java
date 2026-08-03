package com.sedif.sistema_tickets.core.estatusticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO del catalogo de estatus.
 *
 * <p>El {@code id} llega nulo al crear y viene informado al leer, de modo que
 * el mismo record sirve de entrada y de salida para un catalogo tan simple.</p>
 */
public record EstatusRecord(

        Long id,

        @NotBlank(message = "El nombre del estatus es obligatorio.")
        @Size(max = 50, message = "El nombre no puede exceder 50 caracteres.")
        String nombre

) {

    public static EstatusRecord desdeEntidad(Estatus estatus) {
        return new EstatusRecord(estatus.getId(), estatus.getNombre());
    }
}
