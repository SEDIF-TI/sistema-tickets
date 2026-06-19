package com.sedif.sistema_tickets.core.documento;

public record ArticuloDTO(
        Integer cantidad,
        String unidad,
        String descripcion
) {}