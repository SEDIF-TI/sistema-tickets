package com.sedif.sistema_tickets.core.usuarios;
/**
 * Estructura para recibir los datos de creación o actualización de un usuario.
 */
public record UsuarioRequest(
        String nombre,
        String correo,
        String password,
        Long rolId,
        // Solo será obligatorio si el rol es AREA
        Long areaId,
        boolean disponibleSoporte
) {}