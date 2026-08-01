package com.sedif.sistema_tickets.security.auth;

import com.sedif.sistema_tickets.core.usuarios.VistaDTO;

import java.util.List;

/**
 * Datos de sesion devueltos tras un login correcto.
 *
 * <p>Contiene el access token y lo minimo que el frontend necesita para pintar
 * la interfaz. <b>Nunca</b> debe incluir la contrasena ni su hash.</p>
 *
 * <p>Los nombres de los campos se conservan tal cual estaban en el anterior
 * {@code AuthResponseRecord} para no romper el contrato con el cliente: solo
 * cambia el nombre de la clase.</p>
 */
public record JwtResponse(
        Long usuarioId,
        String nombre,
        String rol,
        String token,
        String mensaje,
        List<VistaDTO> vistas,
        Long areaId
) {}
