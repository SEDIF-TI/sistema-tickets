package com.sedif.sistema_tickets.core.taller;

import com.sedif.sistema_tickets.exception.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Taller de reparaciones: equipos que el area de TI recibe para diagnostico y
 * reparacion, con su historial.
 *
 * <p>El {@code @PreAuthorize} de clase restringe todo el controlador a
 * ADMINISTRADOR y SOPORTE. Es la unica proteccion efectiva de estas rutas: el
 * prefijo {@code /api/taller} no encaja en ningun patron de SecurityConfig, que
 * solo exigiria sesion iniciada.</p>
 *
 * <p>No se declara {@code @CrossOrigin}: los origenes permitidos los decide
 * WebConfig para todo el proyecto, y anotarlo aqui anularia esa politica.</p>
 *
 * <p>Los errores no se capturan en los metodos. Se dejan subir para que
 * GlobalExceptionHandler los traduzca a una respuesta uniforme sin filtrar al
 * cliente detalles internos como nombres de clase o mensajes de Hibernate.</p>
 */
@RestController
@RequestMapping("/api/taller")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SOPORTE')")
public class EquipoReparacionResource {

    private final EquipoReparacionService equipoService;

    /**
     * Listado del taller, paginado en la base de datos, con busqueda y filtro
     * de estado opcionales. Por defecto muestra los ingresos mas recientes.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<com.sedif.sistema_tickets.exception.PageResponse<EquipoReparacionDTO>>> obtenerTodos(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String estado,
            @org.springframework.data.web.PageableDefault(
                    size = 10, sort = "fechaCreacion",
                    direction = org.springframework.data.domain.Sort.Direction.DESC)
            org.springframework.data.domain.Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok(
                equipoService.listarPaginado(busqueda, estado, pageable)));
    }

    /**
     * Catalogo de estados del taller, servido desde el enum
     * {@code EstadoTaller}.
     *
     * <p>La pantalla llena su desplegable con esta respuesta en lugar de
     * mantener su propia lista: asi los valores que ofrece son exactamente los
     * que el endpoint de cambio de estado admite.</p>
     */
    @GetMapping("/estados")
    public ResponseEntity<ApiResponse<List<com.sedif.sistema_tickets.core.ticket.CatalogoResponse>>> obtenerEstados() {
        List<com.sedif.sistema_tickets.core.ticket.CatalogoResponse> estados =
                java.util.Arrays.stream(com.sedif.sistema_tickets.util.enums.EstadoTaller.values())
                        .map(e -> new com.sedif.sistema_tickets.core.ticket.CatalogoResponse(
                                e.name(), e.getEtiqueta()))
                        .toList();
        return ResponseEntity.ok(ApiResponse.ok(estados));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EquipoReparacionDTO>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(equipoService.obtenerPorId(id)));
    }

    /** Registra la entrada de un equipo al taller. */
    @PostMapping
    public ResponseEntity<ApiResponse<EquipoReparacionDTO>> registrarIngreso(
            @Valid @RequestBody EquipoReparacionRequest request,
            @RequestParam(required = false) Long tecnicoId) {

        EquipoReparacionDTO creado = equipoService.registrarIngreso(request, tecnicoId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(creado, "Equipo registrado en el taller."));
    }

    /** Actualiza diagnostico, solucion y datos del equipo. */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EquipoReparacionDTO>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody EquipoReparacionRequest request,
            @RequestParam(required = false) Long tecnicoId) {

        return ResponseEntity.ok(ApiResponse.ok(
                equipoService.actualizar(id, request, tecnicoId), "Registro actualizado."));
    }

    /** Avanza el equipo en su ciclo de vida dentro del taller. */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<ApiResponse<EquipoReparacionDTO>> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambioEstadoRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                equipoService.cambiarEstado(id, request.estado()), "Estado actualizado."));
    }
}
