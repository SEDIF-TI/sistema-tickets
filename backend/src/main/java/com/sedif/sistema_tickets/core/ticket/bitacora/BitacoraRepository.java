package com.sedif.sistema_tickets.core.ticket.bitacora;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface BitacoraRepository extends JpaRepository<Bitacora, Long> {

    /** Linea de tiempo de un ticket: del movimiento mas reciente al primero. */
    List<Bitacora> findByTicketIdOrderByFechaCreacionDesc(Long ticketId);
}