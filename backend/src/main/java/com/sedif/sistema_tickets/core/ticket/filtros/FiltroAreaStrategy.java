package com.sedif.sistema_tickets.core.ticket.filtros;

import com.sedif.sistema_tickets.core.ticket.Ticket;
import com.sedif.sistema_tickets.core.ticket.TicketRepository;
import com.sedif.sistema_tickets.core.usuarios.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component("AREA")
@RequiredArgsConstructor
public class FiltroAreaStrategy implements TicketFiltroStrategy {
    private final TicketRepository ticketRepository;

    @Override
    public List<Ticket> obtenerTickets(Usuario usuario) {
        return ticketRepository.findByUsuarioArea_Area_Id(usuario.getArea().getId());
    }
}