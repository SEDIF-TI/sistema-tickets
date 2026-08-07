package com.sedif.sistema_tickets.core.actividad;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Actividades del plan anual de trabajo del personal del area de TI.
 *
 * <p>El alta toma la identidad del token y no de un campo del cuerpo, de modo
 * que nadie pueda atribuir actividades a otra persona y falsear el avance del
 * plan. El listado completo del area queda reservado al administrador; cada
 * tecnico consulta las suyas por {@code /mis-actividades}.</p>
 */
@RestController
@RequestMapping("/api/v1/actividades-extras")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SOPORTE')")
public class ActividadExtraResource {

    private final ActividadExtraService actividadExtraService;

    /** Registra una actividad a nombre del usuario autenticado. */
    @PostMapping
    public ResponseEntity<ApiResponse<ActividadExtra>> registrarActividad(
            @Valid @RequestBody ActividadExtraRequest request,
            Authentication authentication) {

        ActividadExtra guardada = actividadExtraService.registrar(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(guardada, "Actividad registrada correctamente."));
    }

    /** Historial completo del area. Solo ADMINISTRADOR. */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<PageResponse<ActividadExtra>>> listarTodas(
            @PageableDefault(size = 10, sort = "fechaActividad", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok(actividadExtraService.listarPaginado(pageable)));
    }

    /** Actividades propias del usuario autenticado. */
    @GetMapping("/mis-actividades")
    public ResponseEntity<ApiResponse<PageResponse<ActividadExtra>>> listarMisActividades(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "fechaActividad", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok(
                actividadExtraService.listarMisActividades(authentication.getName(), pageable)));
    }
}
