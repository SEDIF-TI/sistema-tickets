package com.sedif.sistema_tickets.core.area;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST que expone los endpoints para la gestión de áreas.
 */
@RestController
@RequestMapping("/api/areas")
@RequiredArgsConstructor // Lombok: Genera automáticamente el constructor para inyectar AreaService
public class AreaResource {

    private final AreaService areaService;

    // Se eliminó el constructor manual public AreaResource(...)

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
}