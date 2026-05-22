package com.sedif.sistema_tickets.core.area;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/areas")
public class AreaResource {

    private final AreaService areaService;

    public AreaResource(AreaService areaService) {
        this.areaService = areaService;
    }

    @PostMapping
    public ResponseEntity<Area> registrarArea(@RequestBody AreaRecord record) {
        Area areaCreada = areaService.crearArea(record);
        return ResponseEntity.ok(areaCreada);
    }

    @GetMapping
    public ResponseEntity<List<Area>> obtenerAreas() {
        return ResponseEntity.ok(areaService.listarAreas());
    }
}