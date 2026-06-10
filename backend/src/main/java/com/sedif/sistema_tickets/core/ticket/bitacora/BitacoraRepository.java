package com.sedif.sistema_tickets.core.ticket.bitacora;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BitacoraRepository extends JpaRepository<Bitacora, Long> {
    // Método preparado para cuando necesitemos ver la línea de tiempo de un ticket
    List<Bitacora> findByTicketIdOrderByFechaCreacionDesc(Long ticketId);
}