package com.sedif.sistema_tickets.core.usuarios;
/**
 * DTO específico para transportar los datos de actualización general
 * de un usuario por parte del administrador.
 */
public record ActualizarUsuarioRequest(
        String nombre,
        String correo,
        Long rolId, // <--- Este debe ser el nombre
        Long areaId
) {}