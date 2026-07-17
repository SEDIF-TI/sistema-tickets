package com.sedif.sistema_tickets.core.ticket;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // <-- IMPORTANTE: Faltaba esta
import org.springframework.web.bind.annotation.*;
import com.sedif.sistema_tickets.core.estatusticket.Estatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketResource {

    private final TicketService ticketService; // <-- Asegúrate de tener este campo final
    private final TicketRepository ticketRepository; // <-- Asegúrate de tener este campo final

    @PostMapping
    public ResponseEntity<TicketResponse> crearTicket(@RequestBody TicketRequestRecord request, Authentication authentication) {
        // Si authentication es null, el filtro JWT no está funcionando
        System.out.println("DEBUG: Usuario autenticado: " + (authentication != null ? authentication.getName() : "NULO"));
        
        String correoUsuario = authentication.getName();
        TicketResponse nuevoTicket = ticketService.crearTicket(request, correoUsuario);
        return ResponseEntity.ok(nuevoTicket);
    }

    @GetMapping
    public ResponseEntity<List<TicketResponse>> obtenerTodos(Authentication authentication) {
        // Tomamos quién es el usuario que está pidiendo ver la tabla
        String correoUsuario = authentication.getName();
        
        // Llamamos a la lógica inteligente
        return ResponseEntity.ok(ticketService.obtenerTicketsParaBandeja(correoUsuario));
    }

    @PutMapping("/{id}/finalizar")
    public ResponseEntity<TicketResponse> finalizarTicket(@PathVariable Long id, Authentication authentication) {
        // Tomamos el correo del usuario logueado gracias al token
        String correoUsuario = authentication.getName();
        
        // Llamamos al servicio
        TicketResponse ticketFinalizado = ticketService.finalizarTicketPorEmpleado(id, correoUsuario);
        
        return ResponseEntity.ok(ticketFinalizado);
    }

    @GetMapping("/mis-tickets")
    public ResponseEntity<List<TicketResponse>> obtenerHistorialArea(Authentication authentication) {
        String correoUsuario = authentication.getName();
        List<TicketResponse> historial = ticketService.obtenerTicketsDeMiArea(correoUsuario);
        return ResponseEntity.ok(historial);
    }

    @PutMapping("/{id}/atender")
    public ResponseEntity<TicketResponse> atenderTicket(@PathVariable Long id) {
        TicketResponse ticketAtendido = ticketService.atenderTicket(id);
        return ResponseEntity.ok(ticketAtendido);
    }

    @PutMapping("/{id}/resolver")
    public ResponseEntity<TicketResponse> resolverTicket(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        String justificacion = "Sin justificación";
        Integer planTrabajo = null;
        
        // EXTRAEMOS LA JUSTIFICACIÓN
        if (payload.containsKey("justificacion") && payload.get("justificacion") != null) {
            justificacion = payload.get("justificacion").toString();
        }
        
        // EXTRAEMOS EL PLAN DE TRABAJO
        if (payload.containsKey("planTrabajoClave") && payload.get("planTrabajoClave") != null) {
            planTrabajo = Integer.parseInt(payload.get("planTrabajoClave").toString());
        }
        
        // DELEGAMOS AL SERVICIO
        TicketResponse ticketResuelto = ticketService.resolverTicket(id, justificacion, planTrabajo);
        
        return ResponseEntity.ok(ticketResuelto);
    }
}
record ResolucionRequest(String justificacion) {}