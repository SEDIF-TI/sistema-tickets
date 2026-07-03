package com.sedif.sistema_tickets.core.documento;

import java.util.List;

public record MantenimientoPreventivoRequest(
    String fechaInicio,
    String fechaFin,
    String nombreTecnico,
    String departamento,
    List<EquipoMantenimientoDTO> equipos
) {}