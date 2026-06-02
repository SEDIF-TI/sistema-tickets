package com.sedif.sistema_tickets.core.ticket.filtros;

import com.sedif.sistema_tickets.core.ticket.Ticket;
import com.sedif.sistema_tickets.core.usuarios.Usuario;
import java.util.List;

public interface TicketFiltroStrategy {
    List<Ticket> obtenerTickets(Usuario usuario);
}