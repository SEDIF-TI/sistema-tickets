package com.sedif.sistema_tickets.core.area;

import com.sedif.sistema_tickets.exception.ApiResponse;
import com.sedif.sistema_tickets.exception.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

    /**
     * Listado paginado del panel, con busqueda y filtro de estado resueltos en
     * la base de datos.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AreaResponse>>> obtenerAreas(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Boolean activo,
            @PageableDefault(size = 10, sort = "nombre", direction = Sort.Direction.ASC)
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok(
                areaService.listarAreasPaginado(busqueda, activo, pageable)));
    }

    /**
     * Catalogo completo sin paginar, para los selectores de area de otros
     * formularios (alta de usuario, por ejemplo).
     */
    @GetMapping("/todas")
    public ResponseEntity<ApiResponse<List<AreaResponse>>> obtenerTodas() {
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
