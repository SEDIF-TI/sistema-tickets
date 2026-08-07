package com.sedif.sistema_tickets.core.ticket.filtros;


import com.sedif.sistema_tickets.core.ticket.Ticket;
import com.sedif.sistema_tickets.core.ticket.TicketRepository;
import com.sedif.sistema_tickets.core.usuarios.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Vision PERSONAL: los tickets asignados al tecnico, del mas reciente al mas
 * antiguo. Es la del rol SOPORTE, que ve su propia carga de trabajo.
 */
// El nombre del componente debe coincidir exactamente con el nivel de vision
// guardado en el rol: es la clave con la que TicketService lo localiza.
@Component("PERSONAL")
@RequiredArgsConstructor
public class FiltroPersonalStrategy implements TicketFiltroStrategy {
    private final TicketRepository ticketRepository;

    @Override
    public List<Ticket> obtenerTickets(Usuario usuario) {
        return ticketRepository.findByUsuarioSoporte_IdOrderByFechaCreacionDesc(usuario.getId());
    }
}