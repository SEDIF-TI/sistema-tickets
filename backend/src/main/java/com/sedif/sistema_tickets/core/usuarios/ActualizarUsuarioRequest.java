package com.sedif.sistema_tickets.core.usuarios;
import com.sedif.sistema_tickets.util.enums.RolUsuario;
/**
 * DTO específico para transportar los datos de actualización general
 * de un usuario por parte del administrador.
 */
public record ActualizarUsuarioRequest(
        String nombre,
        String correo,
        RolUsuario rol,
        Long areaId
) {}
