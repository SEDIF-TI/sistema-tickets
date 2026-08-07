package com.sedif.sistema_tickets.core.ticket;

/**
 * Datos minimos de un ticket, con el area indicada por identificador.
 *
 * <p>El alta desde la API usa {@link TicketRequestRecord}, que ademas valida y
 * deduce el area de la sesion autenticada.</p>
 */
public record TicketRecord(
        String titulo,
        String sede,
        String descripcion,
        Long usuarioAreaId // identificador del usuario que levanta el ticket
) {}

