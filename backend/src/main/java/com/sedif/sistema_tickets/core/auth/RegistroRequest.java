package com.sedif.sistema_tickets.core.auth;

import com.sedif.sistema_tickets.util.enums.RolUsuario;

public record RegistroRequest(
        String nombre,
        String correo,
        String username,
        String password,
        RolUsuario rol
) {
}