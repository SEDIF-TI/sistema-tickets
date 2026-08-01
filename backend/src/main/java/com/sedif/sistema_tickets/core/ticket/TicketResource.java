package com.sedif.sistema_tickets.core.ticket;

import com.sedif.sistema_tickets.exception.ApiResponse;
import com.sedif.sistema_tickets.exception.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de gestion de tickets.
 *
 * <p>Cambios de seguridad respecto a la version anterior:</p>
 * <ul>
 *   <li>{@code atender} y {@code resolver} exigen rol SOPORTE o ADMINISTRADOR.
 *       Antes bastaba con encajar en el patron {@code /api/v1/tickets/**} de
 *       SecurityConfig, que incluye a EMPLEADO: cualquier empleado podia
 *       marcar como resueltos los tickets de otras personas.</li>
 *   <li>{@code resolver} recibe un DTO validado en lugar de un
 *       {@code Map<String, Object>} sin tipo, que aceptaba cualquier cosa y
 *       obligaba a parsear a mano.</li>
 *   <li>Las respuestas van envueltas en {@link ApiResponse} y los listados
 *       estan paginados en la base de datos.</li>
 *   <li>Se elimino el volcado por {@code System.out} del usuario autenticado
 *       en cada creacion de ticket.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketResource {

    private final TicketService ticketService;

    /** Alta de ticket: disponible para cualquier usuario autenticado. */
    @PostMapping
    public ResponseEntity<ApiResponse<TicketResponse>> crearTicket(
            @Valid @RequestBody TicketRequestRecord request,
            Authentication authentication) {

        TicketResponse nuevoTicket = ticketService.crearTicket(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(nuevoTicket, "Ticket creado correctamente."));
    }

    /**
     * Bandeja del usuario, paginada. El servicio decide que tickets son
     * visibles segun el nivel de vision del rol: GLOBAL (todos), PERSONAL
     * (asignados al tecnico y creados por el) o AREA (solo su area).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TicketResponse>>> obtenerTodos(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "fechaCreacion", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok(
                ticketService.obtenerTicketsParaBandeja(authentication.getName(), pageable)));
    }

    /** Historial del area del usuario autenticado, paginado. */
    @GetMapping("/mis-tickets")
    public ResponseEntity<ApiResponse<PageResponse<TicketResponse>>> obtenerHistorialArea(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "fechaCreacion", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok(
                ticketService.obtenerTicketsDeMiAreaPaginado(authentication.getName(), pageable)));
    }

    /**
     * Cierre del ticket por parte de quien lo solicito. El servicio comprueba
     * que el ticket pertenezca al area de ese usuario.
     */
    @PutMapping("/{id}/finalizar")
    public ResponseEntity<ApiResponse<TicketResponse>> finalizarTicket(
            @PathVariable Long id, Authentication authentication) {

        return ResponseEntity.ok(ApiResponse.ok(
                ticketService.finalizarTicketPorEmpleado(id, authentication.getName()),
                "Ticket finalizado correctamente."));
    }

    /** Toma del ticket por un tecnico. Exclusivo de SOPORTE y ADMINISTRADOR. */
    @PutMapping("/{id}/atender")
    @PreAuthorize("hasAnyRole('SOPORTE', 'ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<TicketResponse>> atenderTicket(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                ticketService.atenderTicket(id), "Ticket marcado como en atencion."));
    }

    /** Resolucion del ticket. Exclusivo de SOPORTE y ADMINISTRADOR. */
    @PutMapping("/{id}/resolver")
    @PreAuthorize("hasAnyRole('SOPORTE', 'ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<TicketResponse>> resolverTicket(
            @PathVariable Long id,
            @Valid @RequestBody ResolucionRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                ticketService.resolverTicket(id, request.justificacion(), request.planTrabajoClave()),
                "Ticket resuelto correctamente."));
    }
}
