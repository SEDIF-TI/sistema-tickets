package com.sedif.sistema_tickets.core.ticket.filtros;

import com.sedif.sistema_tickets.core.ticket.Ticket;
import com.sedif.sistema_tickets.core.ticket.TicketRepository;
import com.sedif.sistema_tickets.core.usuarios.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Vision GLOBAL: todos los tickets de la institucion, sin importar area ni
 * asignacion. Es la del rol ADMINISTRADOR.
 */
// El nombre del componente debe coincidir exactamente con el nivel de vision
// guardado en el rol: es la clave con la que TicketService lo localiza.
@Component("GLOBAL")
@RequiredArgsConstructor
public class FiltroGlobalStrategy implements TicketFiltroStrategy {
    private final TicketRepository ticketRepository;

    /** El usuario no interviene: esta vision no acota por nadie. */
    @Override
    public List<Ticket> obtenerTickets(Usuario usuario) {
        return ticketRepository.findAll();
    }
}