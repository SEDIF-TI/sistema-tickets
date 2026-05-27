package com.sedif.sistema_tickets.core.ticket;

public record TicketRecord(
        String titulo,
        String descripcion,
        Long usuarioAreaId // ID de quien levanta el ticket
) {}