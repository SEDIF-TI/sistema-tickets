package com.sedif.sistema_tickets.core.dashboard;

import com.sedif.sistema_tickets.exception.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Metricas agregadas del sistema para el panel de administracion.
 *
 * <p>Los datos son agregados de toda la organizacion (tickets por area, carga
 * por tecnico, evolucion diaria), asi que se restringen al rol ADMINISTRADOR
 * con {@code @PreAuthorize}, ademas de la barrera por URL.</p>
 */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class DashboardResource {

    private final DashboardService dashboardService;

    @GetMapping("/metricas")
    public ResponseEntity<ApiResponse<DashboardResponse>> obtenerMetricas() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.obtenerMetricas()));
    }
}
