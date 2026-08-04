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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String estatus,
            @PageableDefault(size = 10, sort = "fechaCreacion", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok(ticketService.obtenerTicketsParaBandeja(
                authentication.getName(), busqueda, estatus, pageable)));
    }

    /** Historial del area del usuario autenticado, paginado. */
    @GetMapping("/mis-tickets")
    public ResponseEntity<ApiResponse<PageResponse<TicketResponse>>> obtenerHistorialArea(
            Authentication authentication,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String estatus,
            @PageableDefault(size = 10, sort = "fechaCreacion", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok(ticketService.obtenerTicketsDeMiAreaPaginado(
                authentication.getName(), busqueda, estatus, pageable)));
    }

    /**
     * Catalogo de metas del plan anual de trabajo.
     *
     * <p>Lo necesita el desplegable de resolucion. Solo tiene sentido para
     * quien puede resolver tickets, asi que se restringe igual que
     * {@code resolver}.</p>
     */
    @GetMapping("/plan-trabajo")
    @PreAuthorize("hasAnyRole('SOPORTE', 'ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<List<PlanTrabajoResponse>>> obtenerPlanTrabajo() {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.obtenerCatalogoPlanTrabajo()));
    }

    /**
     * Encuesta de satisfaccion.
     *
     * <p>La restriccion de que solo pueda calificar quien levanto el ticket la
     * aplica el servicio: depende del ticket concreto, no del rol, asi que no
     * puede expresarse con {@code @PreAuthorize}.</p>
     */
    @PutMapping("/{id}/calificar")
    public ResponseEntity<ApiResponse<TicketResponse>> calificarTicket(
            @PathVariable Long id,
            @Valid @RequestBody EncuestaRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(ApiResponse.ok(
                ticketService.calificarTicket(id, request.calificacion(),
                        request.comentario(), authentication.getName()),
                "Gracias por calificar el servicio."));
    }

    /** Catalogo de calificaciones, para los botones de la encuesta. */
    @GetMapping("/calificaciones")
    public ResponseEntity<ApiResponse<List<CatalogoResponse>>> obtenerCalificaciones() {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.obtenerCatalogoCalificaciones()));
    }

    /**
     * Catalogo de prioridades para el formulario de alta.
     *
     * <p>Disponible para cualquier usuario autenticado: todos levantan
     * tickets.</p>
     */
    @GetMapping("/prioridades")
    public ResponseEntity<ApiResponse<List<CatalogoResponse>>> obtenerPrioridades() {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.obtenerCatalogoPrioridades()));
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

    /**
     * Aviso de que el tecnico va en camino. Exclusivo de SOPORTE y
     * ADMINISTRADOR.
     *
     * <p>El servicio recibe ademas la identidad autenticada: el rol por si solo
     * no basta, porque no impedia que un tecnico moviera el ticket de otro.</p>
     */
    @PutMapping("/{id}/atender")
    @PreAuthorize("hasAnyRole('SOPORTE', 'ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<TicketResponse>> atenderTicket(
            @PathVariable Long id, Authentication authentication) {

        return ResponseEntity.ok(ApiResponse.ok(
                ticketService.atenderTicket(id, authentication.getName()),
                "Se registro que vas en camino a atender el reporte."));
    }

    /** Resolucion del ticket. Exclusivo de SOPORTE y ADMINISTRADOR. */
    @PutMapping("/{id}/resolver")
    @PreAuthorize("hasAnyRole('SOPORTE', 'ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<TicketResponse>> resolverTicket(
            @PathVariable Long id,
            @Valid @RequestBody ResolucionRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(ApiResponse.ok(
                ticketService.resolverTicket(id, request.justificacion(),
                        request.planTrabajoClave(), authentication.getName()),
                "Ticket resuelto correctamente."));
    }
}
