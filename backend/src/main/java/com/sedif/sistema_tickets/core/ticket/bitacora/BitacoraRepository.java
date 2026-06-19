package com.sedif.sistema_tickets.core.ticket.bitacora;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface BitacoraRepository extends JpaRepository<Bitacora, Long> {
    // Método preparado para cuando necesitemos ver la línea de tiempo de un ticket
    List<Bitacora> findByTicketIdOrderByFechaCreacionDesc(Long ticketId);
}