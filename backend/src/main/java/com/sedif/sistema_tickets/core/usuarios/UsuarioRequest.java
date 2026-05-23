package com.sedif.sistema_tickets.core.usuarios;

import com.sedif.sistema_tickets.util.enums.RolUsuario;

/**
 * Estructura para recibir los datos de creación o actualización de un usuario.
 */
public record UsuarioRequest(
        String nombre,
        String correo,
        String password,
        RolUsuario rol,
        // Solo será obligatorio si el rol es AREA
        Long areaId
) {}