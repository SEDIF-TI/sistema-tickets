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
 * <p>Correcciones de seguridad respecto a la version anterior:</p>
 * <ul>
 *   <li><b>{@code @CrossOrigin(origins = "*")} eliminado.</b> Anulaba la
 *       politica CORS del proyecto y permitia que cualquier sitio web
 *       consultara este endpoint desde el navegador de un usuario con sesion
 *       abierta. El origen permitido lo decide WebConfig.</li>
 *   <li>No exigia ningun rol y su ruta {@code /api/taller} quedaba fuera de
 *       todo patron protegido de SecurityConfig: bastaba estar autenticado.</li>
 *   <li>Los {@code try/catch} que devolvian {@code Map.of("mensaje", ...)} con
 *       el texto de la excepcion filtraban detalles internos al cliente
 *       (nombres de clase, mensajes de Hibernate). Ahora la traduccion la hace
 *       GlobalExceptionHandler, que no expone la causa tecnica.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/taller")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SOPORTE')")
public class EquipoReparacionResource {

    private final EquipoReparacionService equipoService;

    /** Listado del taller, con filtro opcional por texto. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<EquipoReparacionDTO>>> obtenerTodos(
            @RequestParam(required = false) String filtro) {

        List<EquipoReparacionDTO> resultado =
                (filtro != null && !filtro.isBlank())
                        ? equipoService.buscarPorFiltro(filtro)
                        : equipoService.obtenerTodos();

        return ResponseEntity.ok(ApiResponse.ok(resultado));
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
