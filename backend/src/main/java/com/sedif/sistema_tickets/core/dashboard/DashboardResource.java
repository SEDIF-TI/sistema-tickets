package com.sedif.sistema_tickets.core.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class DashboardResource {

    private final DashboardService dashboardService;

    @GetMapping("/metricas")
    public ResponseEntity<DashboardResponse> obtenerMetricas() {
        return ResponseEntity.ok(dashboardService.obtenerMetricas());
    }
}