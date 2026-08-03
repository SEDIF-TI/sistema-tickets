package com.sedif.sistema_tickets.core.area;

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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Administracion del catalogo de areas.
 *
 * <p>La ruta ya estaba bajo {@code /api/v1/admin/**}, protegida por la regla
 * de URL de {@code SecurityConfig}. Se anade {@code @PreAuthorize} a nivel de
 * clase como segunda barrera: si algun dia cambia el patron de URL, la
 * proteccion viaja con el propio controlador.</p>
 */
@RestController
@RequestMapping("/api/v1/admin/areas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AreaResource {

    private final AreaService areaService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AreaResponse>>> obtenerAreas() {
        return ResponseEntity.ok(ApiResponse.ok(areaService.listarAreas()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AreaResponse>> registrarArea(
            @Valid @RequestBody AreaRecord record) {
        AreaResponse creada = areaService.crearArea(record);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(creada, "Area registrada correctamente."));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AreaResponse>> actualizarArea(
            @PathVariable Long id,
            @Valid @RequestBody AreaRecord record) {
        return ResponseEntity.ok(ApiResponse.ok(
                areaService.actualizarArea(id, record), "Area actualizada correctamente."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminarArea(@PathVariable Long id) {
        areaService.eliminarArea(id);
        return ResponseEntity.ok(ApiResponse.ok("Area eliminada correctamente."));
    }

    @PatchMapping("/{id}/soporte-fijo")
    public ResponseEntity<ApiResponse<AreaResponse>> asignarSoporteFijo(
            @PathVariable Long id,
            @Valid @RequestBody SoporteFijoRequestRecord request) {
        return ResponseEntity.ok(ApiResponse.ok(
                areaService.asignarSoporteFijo(id, request), "Soporte fijo asignado correctamente."));
    }
}
