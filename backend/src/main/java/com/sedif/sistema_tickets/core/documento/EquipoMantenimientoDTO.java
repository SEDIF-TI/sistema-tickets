package com.sedif.sistema_tickets.core.documento;

public record EquipoMantenimientoDTO(
    String area,
    String usuarioResponsable,
    String tipoCpu,
    String marca,
    String modelo,
    String numeroSerie,
    String memoriaRam,
    String capacidadDisco,
    String numeroInventario
) {}