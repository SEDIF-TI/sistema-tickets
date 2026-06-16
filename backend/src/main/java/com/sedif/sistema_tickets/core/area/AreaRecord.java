package com.sedif.sistema_tickets.core.area;

public record AreaRecord(
        String nombre,
        Boolean activo,
        Boolean prioritaria
) {
}