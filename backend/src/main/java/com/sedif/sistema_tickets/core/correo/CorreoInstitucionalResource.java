package com.sedif.sistema_tickets.core.correo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/correos")
@CrossOrigin(origins = "*")
public class CorreoInstitucionalResource {

    private final CorreoInstitucionalService correoService;

    @Autowired
    public CorreoInstitucionalResource(CorreoInstitucionalService correoService) {
        this.correoService = correoService;
    }

    @GetMapping
    public ResponseEntity<List<CorreoInstitucional>> obtenerTodos(
            @RequestParam(required = false) String filtro,
            @RequestParam(required = false) String estado) {

        // Delega la evaluación unificada de filtro y estado al servicio
        return ResponseEntity.ok(correoService.buscarPorFiltroYEstado(filtro, estado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CorreoInstitucional> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(correoService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody CorreoInstitucional correo) {
        try {
            CorreoInstitucional creado = correoService.crear(correo);
            return ResponseEntity.status(HttpStatus.CREATED).body(creado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("mensaje", "Error al guardar en base de datos: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody CorreoInstitucional correo) {
        try {
            CorreoInstitucional actualizado = correoService.actualizar(id, correo);
            return ResponseEntity.ok(actualizado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("mensaje", "Error al actualizar en base de datos: " + e.getMessage()));
        }
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String nuevoEstado = body.get("estado");
        if (nuevoEstado == null || nuevoEstado.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "El estado es requerido."));
        }
        CorreoInstitucional actualizado = correoService.cambiarEstado(id, nuevoEstado);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        correoService.eliminar(id);
        return ResponseEntity.ok(Map.of("mensaje", "Registro de correo eliminado correctamente."));
    }
}