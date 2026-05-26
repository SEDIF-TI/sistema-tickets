package com.sedif.sistema_tickets.core.usuarios;

/**
 * Estructura para recibir la petición de cambio de disponibilidad de un usuario de soporte.
 */
public record DisponibilidadRequest(
        Boolean disponible
) {}