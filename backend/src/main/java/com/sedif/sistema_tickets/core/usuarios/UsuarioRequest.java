package com.sedif.sistema_tickets.core.usuarios;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para dar de alta un usuario desde el panel de administracion.
 *
 * <p>El campo {@code password} que existia antes se elimino: nunca se usaba,
 * porque {@code UsuarioService} genera una contrasena temporal aleatoria.
 * Aceptarlo desde el cliente solo abria la puerta a que alguien fijara una
 * contrasena conocida de antemano.</p>
 */
public record UsuarioRequest(

        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 150, message = "El nombre no puede exceder 150 caracteres.")
        String nombre,

        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no tiene un formato valido.")
        @Size(max = 100, message = "El correo no puede exceder 100 caracteres.")
        String correo,

        @Size(max = 50, message = "El nombre de usuario no puede exceder 50 caracteres.")
        String username,

        @NotNull(message = "Debe seleccionar un rol.")
        Long rolId,

        // Obligatorio solo para los roles ligados a un area; lo valida el servicio.
        Long areaId,

        Boolean disponibleSoporte

) {}
