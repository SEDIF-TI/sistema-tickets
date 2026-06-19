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

    @PostMapping
    public ResponseEntity<AvisoResponseRecord> crearAviso(@RequestBody AvisoRequestRecord request) {
        AvisoResponseRecord response = avisoService.crearAvisoGlobal(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/activos")
    public ResponseEntity<List<AvisoResponseRecord>> listarAvisosParaAreas() {
        // Este es el endpoint que el frontend consumirá para mostrar los comunicados
        return ResponseEntity.ok(avisoService.obtenerAvisosActivos());
    }

    @PutMapping("/{id}/desactivar")
    public ResponseEntity<Void> desactivarAviso(@PathVariable Long id) {
        avisoService.desactivarAviso(id);
        return ResponseEntity.ok().build();
    }
}