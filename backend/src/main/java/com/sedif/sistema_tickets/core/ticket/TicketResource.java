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
        // Si authentication es null, el filtro JWT no está funcionando
        System.out.println("DEBUG: Usuario autenticado: " + (authentication != null ? authentication.getName() : "NULO"));
        
        String correoUsuario = authentication.getName();
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

    @GetMapping("/mis-tickets")
    public ResponseEntity<List<TicketResponse>> obtenerHistorialArea(Authentication authentication) {
        String correoUsuario = authentication.getName();
        List<TicketResponse> historial = ticketService.obtenerTicketsDeMiArea(correoUsuario);
        return ResponseEntity.ok(historial);
    }

    @PutMapping("/{id}/atender")
    public ResponseEntity<Ticket> atenderTicket(@PathVariable Long id) {
        Ticket ticketAtendido = ticketService.atenderTicket(id);
        return ResponseEntity.ok(ticketAtendido);
    }

    @PutMapping("/{id}/resolver")
    public ResponseEntity<Ticket> resolverTicket(@PathVariable Long id, @RequestBody ResolucionRequest request) {
        // Se extrae la justificación del cuerpo de la petición JSON
        Ticket ticketResuelto = ticketService.resolverTicket(id, request.justificacion());
        return ResponseEntity.ok(ticketResuelto);
    }
}
record ResolucionRequest(String justificacion) {}