package com.sedif.sistema_tickets.core.documento;

public record ResguardoRequest(
        String solicitanteNombre,
        String solicitanteNumero,
        String equipoNombre,
        String numeroSerie,
        String accesorios,
        Integer duracionCantidad,
        String duracionTipo
) {}