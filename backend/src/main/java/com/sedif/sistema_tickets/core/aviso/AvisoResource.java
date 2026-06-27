package com.sedif.sistema_tickets.core.aviso;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/avisos")
@RequiredArgsConstructor
public class AvisoResource {

    private final AvisoService avisoService;

    // Crear un nuevo aviso
    @PostMapping
    public ResponseEntity<AvisoResponseRecord> crearAviso(@RequestBody AvisoRequestRecord request) {
        return ResponseEntity.ok(avisoService.crearAvisoGlobal(request));
    }

    // Listar solo activos para el panel de usuarios (MainLayout)
    @GetMapping("/activos")
    public ResponseEntity<List<AvisoResponseRecord>> listarAvisosParaAreas() {
        return ResponseEntity.ok(avisoService.obtenerAvisosActivos());
    }

    // Listar todos los avisos para el Panel del Administrador
    @GetMapping
    public ResponseEntity<List<AvisoResponseRecord>> listarTodos() {
        return ResponseEntity.ok(avisoService.obtenerTodos());
    }

    // Actualizar aviso (Sirve para el interruptor de estado y editar contenido)
    @PutMapping("/{id}")
    public ResponseEntity<AvisoResponseRecord> actualizarAviso(@PathVariable Long id, @RequestBody AvisoRequestRecord request) {
        return ResponseEntity.ok(avisoService.actualizarAviso(id, request));
    }

    // Borrar físicamente el aviso
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarAviso(@PathVariable Long id) {
        avisoService.eliminarAvisoFisico(id);
        return ResponseEntity.ok().build();
    }
}