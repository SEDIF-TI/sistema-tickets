package com.sedif.sistema_tickets.core.ticket;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    
    // Spring Data JPA sigue resolviendo esto navegando de Ticket -> Estatus -> nombre automáticamente
    long countByUsuarioSoporteAndEstatusNombre(Usuario usuarioSoporte, String nombreEstatus);
}