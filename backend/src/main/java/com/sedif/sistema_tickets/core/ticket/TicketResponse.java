package com.sedif.sistema_tickets.core.ticket;

/**
 * Esta clase es un "Data Transfer Object" (DTO)
 * Sirve para enviar al cliente solo la información necesaria
 * y evitar errores de recursión o datos innecesarios.
 */
public record TicketResponse(
    Long id,
    String titulo,
    String descripcion,
    String estatus,
    Long usuarioAreaId,
    Long usuarioSoporteId
) {}