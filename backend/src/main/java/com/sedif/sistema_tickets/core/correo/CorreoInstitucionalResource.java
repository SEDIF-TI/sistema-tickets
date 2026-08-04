package com.sedif.sistema_tickets.core.correo;

import com.sedif.sistema_tickets.exception.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * Control de altas y bajas de correo institucional ante el proveedor.
 *
 * <p>Correcciones de seguridad respecto a la version anterior:</p>
 * <ul>
 *   <li><b>{@code @CrossOrigin(origins = "*")} eliminado.</b> Anulaba la
 *       politica CORS del proyecto: cualquier sitio web podia consultar el
 *       directorio de correos institucionales desde el navegador de un usuario
 *       con sesion abierta. El origen permitido lo decide WebConfig.</li>
 *   <li>No exigia ningun rol y {@code /api/correos} quedaba fuera de todo
 *       patron protegido: bastaba estar autenticado para leer, crear y
 *       eliminar cuentas. Se restringe a ADMINISTRADOR y SOPORTE.</li>
 *   <li>El borrado definitivo se reserva al ADMINISTRADOR: la baja ordinaria
 *       es logica (cambio de estado a BAJA), que conserva la trazabilidad.</li>
 *   <li>Los {@code catch} que devolvian {@code "Error al guardar en base de
 *       datos: " + e.getMessage()} filtraban al cliente mensajes de Hibernate
 *       y nombres de columnas. Ahora responde GlobalExceptionHandler.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/correos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SOPORTE')")
public class CorreoInstitucionalResource {

    private final CorreoInstitucionalService correoService;

    /** Directorio de correos, con filtro por texto y por estado. */
    @GetMapping
    public ResponseEntity<ApiResponse<com.sedif.sistema_tickets.exception.PageResponse<CorreoInstitucional>>> obtenerTodos(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String estado,
            @org.springframework.data.web.PageableDefault(
                    size = 10, sort = "nombre",
                    direction = org.springframework.data.domain.Sort.Direction.ASC)
            org.springframework.data.domain.Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok(
                correoService.listarPaginado(busqueda, estado, pageable)));
    }

    /** Catalogo de estados de la cuenta, servido desde el enum EstadoCorreo. */
    @GetMapping("/estados")
    public ResponseEntity<ApiResponse<List<com.sedif.sistema_tickets.core.ticket.CatalogoResponse>>> obtenerEstados() {
        List<com.sedif.sistema_tickets.core.ticket.CatalogoResponse> estados =
                java.util.Arrays.stream(com.sedif.sistema_tickets.util.enums.EstadoCorreo.values())
                        .map(e -> new com.sedif.sistema_tickets.core.ticket.CatalogoResponse(
                                e.name(), e.getEtiqueta()))
                        .toList();
        return ResponseEntity.ok(ApiResponse.ok(estados));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CorreoInstitucional>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(correoService.obtenerPorId(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CorreoInstitucional>> crear(
            @Valid @RequestBody CorreoRequest correo) {

        CorreoInstitucional creado = correoService.crear(correo);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(creado, "Correo institucional registrado."));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CorreoInstitucional>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody CorreoRequest correo) {

        return ResponseEntity.ok(ApiResponse.ok(
                correoService.actualizar(id, correo), "Correo actualizado correctamente."));
    }

    /** Alta, baja o suspension de la cuenta ante el proveedor. */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<ApiResponse<CorreoInstitucional>> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambioEstadoCorreoRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                correoService.cambiarEstado(id, request.estado()), "Estado actualizado."));
    }

    /**
     * Borrado definitivo del registro.
     *
     * <p>Solo ADMINISTRADOR: elimina la trazabilidad de la cuenta. Para dar de
     * baja una cuenta sin perder el historial esta el cambio de estado.</p>
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        correoService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Registro de correo eliminado correctamente."));
    }
}
