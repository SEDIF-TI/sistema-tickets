package com.sedif.sistema_tickets.core.area;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/areas")
@RequiredArgsConstructor
public class AreaResource {

    private final AreaService areaService;

    /**
     * Endpoint para registrar una nueva área.
     * Método HTTP: POST
     * URL: http://localhost:8080/api/areas
     */
    @PostMapping
    public ResponseEntity<Area> registrarArea(@RequestBody AreaRecord record) {
        Area areaCreada = areaService.crearArea(record);
        return ResponseEntity.ok(areaCreada);
    }

    /**
     * Endpoint para obtener la lista de todas las áreas.
     * Método HTTP: GET
     * URL: http://localhost:8080/api/areas
     */
    @GetMapping
    public ResponseEntity<List<Area>> obtenerAreas() {
        return ResponseEntity.ok(areaService.listarAreas());
    }

    /**
     * Endpoint para actualizar un área existente.
     * Método HTTP: PUT
     * URL: http://localhost:8080/api/areas/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Area> actualizarArea(@PathVariable Long id, @RequestBody AreaRecord record) {
        Area areaActualizada = areaService.actualizarArea(id, record);
        return ResponseEntity.ok(areaActualizada);
    }

    /**
     * Endpoint para realizar el borrado lógico de un área.
     * Método HTTP: DELETE
     * URL: http://localhost:8080/api/areas/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarArea(@PathVariable Long id) {
        areaService.eliminarArea(id);
        return ResponseEntity.noContent().build();
    }
}