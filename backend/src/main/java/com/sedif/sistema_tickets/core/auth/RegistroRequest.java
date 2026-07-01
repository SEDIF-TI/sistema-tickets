package com.sedif.sistema_tickets.core.auth;

public record RegistroRequest(
        String nombre,
        String apellidoPaterno,
        String apellidoMaterno,
        String correo,
        String username,
        String password,
        Long rolId
) {}