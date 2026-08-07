package com.sedif.sistema_tickets.core.ticket.filtros;

import com.sedif.sistema_tickets.core.ticket.Ticket;
import com.sedif.sistema_tickets.core.ticket.TicketRepository;
import com.sedif.sistema_tickets.core.usuarios.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Vision AREA: los tickets levantados por personal del area a la que pertenece
 * el usuario, sea quien sea el tecnico que los atiende.
 */
// El nombre del componente debe coincidir exactamente con el nivel de vision
// guardado en el rol: es la clave con la que TicketService lo localiza.
@Component("AREA")
@RequiredArgsConstructor
public class FiltroAreaStrategy implements TicketFiltroStrategy {
    private final TicketRepository ticketRepository;

   /** Requiere que el usuario tenga area asignada. */
   @Override
    public List<Ticket> obtenerTickets(Usuario usuario) {
        return ticketRepository.findByUsuarioAreaAreaId(usuario.getArea().getId());
    }
}