package com.sedif.sistema_tickets.core.usuarios;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de actualizacion de un usuario desde el panel de administracion.
 *
 * <p>No incluye la contrasena a proposito: el restablecimiento tiene su propio
 * endpoint, que genera una clave temporal en el servidor.</p>
 */
public record ActualizarUsuarioRequest(

        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 150, message = "El nombre no puede exceder 150 caracteres.")
        String nombre,

        @Size(max = 255, message = "El apellido paterno no puede exceder 255 caracteres.")
        String apellidoPaterno,

        @Size(max = 255, message = "El apellido materno no puede exceder 255 caracteres.")
        String apellidoMaterno,

        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no tiene un formato valido.")
        @Size(max = 100, message = "El correo no puede exceder 100 caracteres.")
        String correo,

        Long rolId,

        Long areaId

) {}
