package com.sedif.sistema_tickets.core.auth;

public record AuthResponseRecord(
        Long usuarioId,
        String nombre,
        String rol,
        String token,
        String mensaje
) {}