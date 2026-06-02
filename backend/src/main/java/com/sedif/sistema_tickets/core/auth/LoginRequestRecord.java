package com.sedif.sistema_tickets.core.auth;

public record LoginRequestRecord(
        String identificador, // Acepta correo o nombre de usuario
        String password
) {}