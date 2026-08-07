package com.sedif.sistema_tickets.core.usuarios;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para que un usuario cambie su propia contrasena.
 *
 * <p>Exigir {@code passwordActual} obliga a demostrar la identidad ante el
 * cambio: quien consiguiera un token valido sin conocer la clave original no
 * puede apoderarse de la cuenta de forma permanente.</p>
 *
 * <p>El requisito encaja tambien con el primer acceso: en ese momento el
 * usuario acaba de recibir su contrasena temporal, de modo que puede aportarla
 * igual que cualquier otra.</p>
 */
public record CambioPasswordRequest(

        @NotBlank(message = "Debe proporcionar su contrasena actual.")
        @Size(max = 100, message = "La contrasena no puede exceder 100 caracteres.")
        String passwordActual,

        @NotBlank(message = "La nueva contrasena es obligatoria.")
        @Size(min = 8, max = 100, message = "La nueva contrasena debe tener entre 8 y 100 caracteres.")
        @Pattern(regexp = ".*[a-zA-Z].*", message = "La contrasena debe incluir al menos una letra.")
        @Pattern(regexp = ".*\\d.*", message = "La contrasena debe incluir al menos un numero.")
        String nuevaPassword

) {}
