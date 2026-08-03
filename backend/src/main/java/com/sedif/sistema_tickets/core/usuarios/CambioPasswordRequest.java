package com.sedif.sistema_tickets.core.usuarios;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para que un usuario cambie su propia contrasena.
 *
 * <p>Antes este record solo llevaba {@code nuevaPassword}: no se pedia la
 * contrasena actual ni se validaba la nueva. Cualquiera que consiguiera un
 * token (mediante XSS, o encontrando una sesion abierta) podia apoderarse de
 * la cuenta de forma permanente cambiando la clave sin conocer la original.</p>
 *
 * <p>Exigir {@code passwordActual} tambien encaja con el primer acceso: en ese
 * momento el usuario acaba de recibir su contrasena temporal, de modo que
 * puede aportarla igual que cualquier otra.</p>
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
