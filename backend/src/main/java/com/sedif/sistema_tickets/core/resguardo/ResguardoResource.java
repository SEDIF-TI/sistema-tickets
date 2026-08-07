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
 * <p>El {@code @PreAuthorize} de clase restringe todo el controlador a
 * ADMINISTRADOR y SOPORTE. Es la unica proteccion efectiva de estas rutas: no
 * encajan en ningun patron de SecurityConfig, que solo exigiria sesion
 * iniciada, de modo que sin esta anotacion cualquier EMPLEADO podria consultar
 * y crear resguardos de toda la institucion.</p>
 *
 * <p>El proceso de vencimientos no se expone por HTTP: lo dispara la tarea
 * programada de {@link ResguardoService}, que ademas envia avisos por Telegram.
 * Un endpoint que lo lanzara a peticion permitiria provocar ese envio masivo
 * desde fuera.</p>
 *
 * <p>Los errores no se capturan aqui: se dejan subir para que
 * GlobalExceptionHandler los traduzca conservando su mensaje, en lugar de
 * devolver un {@code 400} con cuerpo vacio que el cliente no puede
 * interpretar.</p>
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

    /**
     * Historial de resguardos, paginado en la base de datos, con busqueda y
     * filtro de estado opcionales. Por defecto muestra los mas recientes.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ResguardoResponse>>> listarTodos(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String busqueda,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String estado,
            @PageableDefault(size = 10, sort = "fechaCreacion", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok(
                resguardoService.listarPaginado(busqueda, estado, pageable)));
    }

    /**
     * Marca el equipo como devuelto y cierra el resguardo. El servicio rechaza
     * el intento si ya estaba devuelto.
     */
    @PutMapping("/{id}/devolucion")
    public ResponseEntity<ApiResponse<ResguardoResponse>> devolverEquipo(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                resguardoService.devolverEquipo(id), "Devolucion registrada correctamente."));
    }
}
