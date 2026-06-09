package com.sedif.sistema_tickets.core.ticket;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // <-- IMPORTANTE: Faltaba esta
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketResource {

    private final TicketService ticketService; // <-- Asegúrate de tener este campo final

    @PostMapping
    public ResponseEntity<Ticket> crearTicket(@RequestBody TicketRequestRecord request, Authentication authentication) {
        // Usamos authentication.getName() que ya contiene el correo gracias al JwtAuthenticationFilter
        String correoUsuario = authentication.getName();
        
        System.out.println("LOG: Solicitud POST recibida en /api/v1/tickets. Usuario: " + correoUsuario);
        
        Ticket nuevoTicket = ticketService.crearTicket(request, correoUsuario);
        return ResponseEntity.ok(nuevoTicket);
    }

    @GetMapping
    public ResponseEntity<List<TicketResponse>> obtenerTodos() {
        return ResponseEntity.ok(ticketService.obtenerTodosLosTickets());
    }

    @PutMapping("/{id}/finalizar")
    public ResponseEntity<Ticket> finalizarTicket(@PathVariable Long id, Authentication authentication) {
        // Tomamos el correo del usuario logueado gracias al token
        String correoUsuario = authentication.getName();
        
        // Llamamos al servicio
        Ticket ticketFinalizado = ticketService.finalizarTicketPorEmpleado(id, correoUsuario);
        
        return ResponseEntity.ok(ticketFinalizado);
    }

    @GetMapping("/area")
    public ResponseEntity<List<TicketResponse>> obtenerHistorialArea(Authentication authentication) {
        String correoUsuario = authentication.getName();
        List<TicketResponse> historial = ticketService.obtenerTicketsDeMiArea(correoUsuario);
        return ResponseEntity.ok(historial);
    }
}