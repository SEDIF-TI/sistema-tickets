package com.sedif.sistema_tickets.core.correo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de alta y edicion de una cuenta de correo institucional.
 *
 * <p>Los endpoints recibian la entidad {@link CorreoInstitucional} directa.
 * Eso permitia enviar un {@code id} en el cuerpo para apuntar a otro registro
 * (mass assignment) y, sobre todo, fijar el {@code estado} sin pasar por el
 * endpoint de cambio de estado, que es el unico que valida contra el enum
 * {@code EstadoCorreo}. Una cuenta podia quedar asi en un estado inexistente,
 * invisible para los filtros del directorio.</p>
 */
public record CorreoRequest(

        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres.")
        String nombre,

        @NotBlank(message = "El apellido paterno es obligatorio.")
        @Size(max = 100, message = "El apellido paterno no puede exceder 100 caracteres.")
        String apellidoPaterno,

        @Size(max = 100, message = "El apellido materno no puede exceder 100 caracteres.")
        String apellidoMaterno,

        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no tiene un formato valido.")
        @Size(max = 150, message = "El correo no puede exceder 150 caracteres.")
        String correo,

        @NotBlank(message = "El area es obligatoria.")
        @Size(max = 150, message = "El area no puede exceder 150 caracteres.")
        String area,

        @Size(max = 150, message = "El cargo no puede exceder 150 caracteres.")
        String cargo,

        @Size(max = 20, message = "La extension no puede exceder 20 caracteres.")
        String extension,

        @Size(max = 30, message = "La cuota no puede exceder 30 caracteres.")
        String cuotaAlmacenamiento

) {}
