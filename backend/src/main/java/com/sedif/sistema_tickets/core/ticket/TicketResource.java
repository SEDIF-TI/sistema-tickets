package com.sedif.sistema_tickets.core.ticket;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets") // Le agregué /v1/ para que sea consistente con tu AuthResource
@RequiredArgsConstructor
public class TicketResource {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<Ticket> crearTicket(
            @RequestBody TicketRequestRecord request, 
            Authentication authentication // Spring inyecta al usuario logueado aquí mágicamente
    ) {
        // authentication.getName() extrae el correo/username del Token JWT de forma segura
        Ticket nuevoTicket = ticketService.crearTicket(request, authentication.getName());
        return ResponseEntity.ok(nuevoTicket);
    }

    @GetMapping
    public ResponseEntity<List<TicketResponse>> obtenerTodos() {
        return ResponseEntity.ok(ticketService.obtenerTodosLosTickets());
    }
}