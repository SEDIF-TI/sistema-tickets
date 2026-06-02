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

    @PostMapping
    public ResponseEntity<AreaResponse> registrarArea(@RequestBody AreaRecord record) {
        return ResponseEntity.ok(areaService.crearArea(record));
    }

    @GetMapping
    public ResponseEntity<List<AreaResponse>> obtenerAreas() {
        return ResponseEntity.ok(areaService.listarAreas());
    }

    @PutMapping("/{id}")
    public ResponseEntity<AreaResponse> actualizarArea(@PathVariable Long id, @RequestBody AreaRecord record) {
        return ResponseEntity.ok(areaService.actualizarArea(id, record));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarArea(@PathVariable Long id) {
        areaService.eliminarArea(id);
        return ResponseEntity.noContent().build();
    }

    // --- NUEVO ENDPOINT INTEGRADO ---
    @PatchMapping("/{id}/soporte-fijo")
    public ResponseEntity<AreaResponse> asignarSoporteFijo(
            @PathVariable Long id,
            @RequestBody SoporteFijoRequestRecord request) {
        
        AreaResponse response = areaService.asignarSoporteFijo(id, request);
        return ResponseEntity.ok(response);
    }
}