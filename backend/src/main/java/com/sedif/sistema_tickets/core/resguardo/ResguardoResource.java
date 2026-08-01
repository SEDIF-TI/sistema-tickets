package com.sedif.sistema_tickets.core.resguardo;

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
 * Resguardos de equipo: prestamos temporales que el area de TI entrega al
 * personal (equipos de computo, antenas, cables, cargadores).
 *
 * <p>Correcciones de seguridad respecto a la version anterior:</p>
 * <ul>
 *   <li>El controlador no exigia ningun rol y su ruta quedaba fuera de todo
 *       patron protegido de SecurityConfig: bastaba estar autenticado, de modo
 *       que cualquier EMPLEADO podia consultar y crear resguardos de toda la
 *       institucion.</li>
 *   <li>Se elimino {@code POST /test-alertas}, un endpoint de pruebas expuesto
 *       en produccion que permitia a cualquiera disparar el proceso de
 *       vencimientos y el envio masivo de mensajes por Telegram.</li>
 *   <li>{@code devolverEquipo} capturaba la excepcion y devolvia {@code 400}
 *       con cuerpo {@code null}: el cliente recibia un error sin explicacion.
 *       Ahora la traduce GlobalExceptionHandler conservando su mensaje.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/resguardos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SOPORTE')")
public class ResguardoResource {

    private final ResguardoService resguardoService;

    /** Registra la entrega de un equipo en resguardo. */
    @PostMapping
    public ResponseEntity<ApiResponse<ResguardoResponse>> crearResguardo(
            @Valid @RequestBody ResguardoRequest request,
            Authentication authentication) {

        ResguardoResponse creado = resguardoService.crearResguardo(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(creado, "Resguardo registrado correctamente."));
    }

    /** Historial completo de resguardos, paginado y ordenado por fecha. */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ResguardoResponse>>> listarTodos(
            @PageableDefault(size = 10, sort = "fechaCreacion", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok(resguardoService.listarPaginado(pageable)));
    }

    /** Marca el equipo como devuelto y cierra el resguardo. */
    @PutMapping("/{id}/devolucion")
    public ResponseEntity<ApiResponse<ResguardoResponse>> devolverEquipo(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                resguardoService.devolverEquipo(id), "Devolucion registrada correctamente."));
    }
}
