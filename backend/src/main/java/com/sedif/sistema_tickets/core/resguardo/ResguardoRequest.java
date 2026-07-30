package com.sedif.sistema_tickets.core.resguardo;

public record ResguardoRequest(
        String solicitanteNombre,
        String solicitanteNumero, // <-- ESTE CAMPO FALTABA AQUÍ
        String equipoNombre,
        String numeroSerie,
        String accesorios,
        Integer duracionCantidad,
        String duracionTipo,
        String telefono,
        String departamento,
        String numeroInventario,
        String condiciones,
        String entregaNombre
) {}