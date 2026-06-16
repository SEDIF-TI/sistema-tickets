package com.sedif.sistema_tickets.core.ticket;

public record TicketRequestRecord(
        String titulo,
        String sede,
        String descripcion,
        String prioridad
) {}