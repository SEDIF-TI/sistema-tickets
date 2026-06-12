package com.sedif.sistema_tickets.core.usuarios.dto;

public record UsuarioCreacionRequest(
    String nombre,
    String username,
    String correo,
    Long areaId,
    Long rolId,
    boolean disponibleSoporte
) {}