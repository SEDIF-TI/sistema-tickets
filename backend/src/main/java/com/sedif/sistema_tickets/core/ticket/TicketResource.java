package com.sedif.sistema_tickets.core.ticket;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketResource {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<TicketResponse> crearTicket(@RequestBody TicketRecord record) {
        return ResponseEntity.ok(ticketService.crearTicket(record));
    }

    @GetMapping
    public ResponseEntity<List<TicketResponse>> obtenerTodos() {
        return ResponseEntity.ok(ticketService.obtenerTodosLosTickets());
    }
}