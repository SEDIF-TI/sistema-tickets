package com.sedif.sistema_tickets.core.ticket.filtros;

import com.sedif.sistema_tickets.core.ticket.Ticket;
import com.sedif.sistema_tickets.core.usuarios.Usuario;
import java.util.List;

/**
 * Visibilidad de tickets segun el nivel de vision del rol.
 *
 * <p>Cada implementacion se registra como componente cuyo nombre coincide con
 * el nivel guardado en el rol (GLOBAL, AREA o PERSONAL). {@code TicketService}
 * recibe todas en un {@code Map<String, TicketFiltroStrategy>} indexado por ese
 * nombre y elige la que corresponde al usuario, de modo que anadir un nivel
 * nuevo solo exige una clase mas y no tocar el servicio.</p>
 */
public interface TicketFiltroStrategy {

    /** Tickets que ese usuario puede ver, segun el nivel de la estrategia. */
    List<Ticket> obtenerTickets(Usuario usuario);
}