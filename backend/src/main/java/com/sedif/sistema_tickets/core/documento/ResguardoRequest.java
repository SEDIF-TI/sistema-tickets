package com.sedif.sistema_tickets.core.documento;

/**
 * Datos de la responsiva de resguardo de un equipo.
 *
 * <p>La duracion se expresa como cantidad mas unidad (por ejemplo 6 meses),
 * tal como aparece en la columna de vigencia del formato.</p>
 */
public record ResguardoRequest(
        String solicitanteNombre,
        String solicitanteNumero,
        String equipoNombre,
        String numeroSerie,
        String accesorios,
        Integer duracionCantidad,
        String duracionTipo
) {}