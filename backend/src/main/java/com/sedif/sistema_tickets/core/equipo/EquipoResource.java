package com.sedif.sistema_tickets.core.equipo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import java.util.List;

@RestController
@RequestMapping("/api/v1/equipos")
@RequiredArgsConstructor
public class EquipoResource { // <-- CAMBIADO A RESOURCE

    private final EquipoRepository equipoRepository;

    // 1. Endpoint para el Autocomplete del Frontend
    @GetMapping("/buscar")
    public ResponseEntity<List<Equipo>> buscarEquipos(@RequestParam String q) {
        if (q == null || q.trim().isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        
        List<Equipo> resultados = equipoRepository.findByDescripcionContainingIgnoreCase(q.trim());
        return ResponseEntity.ok(resultados);
    }

    // 2. Endpoint "Just In Time" que guarda o actualiza el catálogo silenciosamente
    @PostMapping("/upsert")
    public ResponseEntity<Equipo> registrarOActualizar(@RequestBody Equipo equipoRequest) {
        if (equipoRequest.getDescripcion() == null || equipoRequest.getDescripcion().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Equipo equipoExistente = equipoRepository.findByDescripcionIgnoreCase(equipoRequest.getDescripcion().trim())
                .orElse(new Equipo());

        equipoExistente.setDescripcion(equipoRequest.getDescripcion());
        if (equipoRequest.getMarca() != null) equipoExistente.setMarca(equipoRequest.getMarca());
        if (equipoRequest.getModelo() != null) equipoExistente.setModelo(equipoRequest.getModelo());

        Equipo guardado = equipoRepository.save(equipoExistente);
        return ResponseEntity.ok(guardado);
    }

    // 3. Listar TODOS los equipos para el panel de administración
    @GetMapping
    public ResponseEntity<List<Equipo>> listarTodos() {
        return ResponseEntity.ok(equipoRepository.findAll());
    }

    // 4. Actualizar un equipo manualmente (Admin)
    @PutMapping("/{id}")
    public ResponseEntity<Equipo> actualizarEquipo(@PathVariable Long id, @RequestBody Equipo request) {
        return equipoRepository.findById(id).map(equipo -> {
            equipo.setDescripcion(request.getDescripcion());
            equipo.setMarca(request.getMarca());
            equipo.setModelo(request.getModelo());
            return ResponseEntity.ok(equipoRepository.save(equipo));
        }).orElse(ResponseEntity.notFound().build());
    }

    // 5. Eliminar un equipo del catálogo (Admin)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarEquipo(@PathVariable Long id) {
        if (equipoRepository.existsById(id)) {
            equipoRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}