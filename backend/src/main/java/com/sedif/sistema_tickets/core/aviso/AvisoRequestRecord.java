package com.sedif.sistema_tickets.core.aviso;

public record AvisoRequestRecord(
        String titulo,
        String mensaje,
        Boolean activo,
        Long areaId
) {}