package com.sedif.sistema_tickets.core.taller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/taller")
@CrossOrigin(origins = "*")
public class EquipoReparacionResource {

    private final EquipoReparacionService equipoService;

    @Autowired
    public EquipoReparacionResource(EquipoReparacionService equipoService) {
        this.equipoService = equipoService;
    }

    // Obtenemos todos los equipos o filtramos por término de búsqueda
    @GetMapping
    public ResponseEntity<List<EquipoReparacionDTO>> obtenerTodos(
            @RequestParam(required = false) String filtro) {
        if (filtro != null && !filtro.trim().isEmpty()) {
            return ResponseEntity.ok(equipoService.buscarPorFiltro(filtro));
        }
        return ResponseEntity.ok(equipoService.obtenerTodos());
    }

    // Obtener un registro por ID
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            EquipoReparacionDTO dto = equipoService.obtenerPorId(id);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("mensaje", e.getMessage()));
        }
    }

    // Registrar nuevo equipo en taller
    @PostMapping
    public ResponseEntity<?> registrarIngreso(
            @RequestBody EquipoReparacion equipo,
            @RequestParam(required = false) Long tecnicoId) {
        try {
            EquipoReparacionDTO creado = equipoService.registrarIngreso(equipo, tecnicoId);
            return ResponseEntity.status(HttpStatus.CREATED).body(creado);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("mensaje", "Error al registrar el ingreso al taller: " + e.getMessage()));
        }
    }

    // Actualizar datos del equipo/diagnóstico
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(
            @PathVariable Long id,
            @RequestBody EquipoReparacion equipo,
            @RequestParam(required = false) Long tecnicoId) {
        try {
            EquipoReparacionDTO actualizado = equipoService.actualizar(id, equipo, tecnicoId);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("mensaje", "Error al actualizar el registro: " + e.getMessage()));
        }
    }

    // Cambiar rápidamente el estado del ciclo de vida (RECIBIDO, EN_DIAGNOSTICO, ENTREGADO, etc.)
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String nuevoEstado = body.get("estado");
        if (nuevoEstado == null || nuevoEstado.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "El estado es requerido."));
        }
        try {
            EquipoReparacionDTO actualizado = equipoService.cambiarEstado(id, nuevoEstado);
            return ResponseEntity.ok(actualizado);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("mensaje", "Error al cambiar estado: " + e.getMessage()));
        }
    }
}