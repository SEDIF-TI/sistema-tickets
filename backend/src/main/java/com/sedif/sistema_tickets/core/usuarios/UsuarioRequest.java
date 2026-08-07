package com.sedif.sistema_tickets.core.usuarios;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para dar de alta un usuario desde el panel de administracion.
 *
 * <p>No incluye contrasena: {@code UsuarioService} genera una temporal
 * aleatoria en el servidor. Aceptarla desde el cliente permitiria fijar una
 * clave conocida de antemano.</p>
 */
public record UsuarioRequest(

        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 150, message = "El nombre no puede exceder 150 caracteres.")
        String nombre,

        // Los apellidos viajan aparte y no dentro del nombre: los documentos
        // oficiales (resguardos, memorandos) los imprimen por separado.
        @Size(max = 255, message = "El apellido paterno no puede exceder 255 caracteres.")
        String apellidoPaterno,

        @Size(max = 255, message = "El apellido materno no puede exceder 255 caracteres.")
        String apellidoMaterno,

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
