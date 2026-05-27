package com.sedif.sistema_tickets.core.ticket;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    
    // IMPORTANTE: 'usuarioSoporte' debe coincidir exactamente con el nombre del atributo 
    // en la clase Ticket.java.
    long countByUsuarioSoporteAndEstatus(Usuario usuarioSoporte, String estatus);
}