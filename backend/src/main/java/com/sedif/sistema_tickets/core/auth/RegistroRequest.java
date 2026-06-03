package com.sedif.sistema_tickets.core.auth;

public record RegistroRequest(
        String nombre,
        String correo,
        String username,
        String password,
        Long rolId
) {}