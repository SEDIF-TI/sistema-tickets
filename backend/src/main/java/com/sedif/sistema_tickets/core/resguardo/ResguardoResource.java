package com.sedif.sistema_tickets.core.resguardo;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map; // 👈 AGREGADO: Necesario para Map.of

@RestController
@RequestMapping("/api/v1/resguardos")
@RequiredArgsConstructor
public class ResguardoResource {

    private final ResguardoService resguardoService;

    // 1. Crear un nuevo resguardo
    @PostMapping
    public ResponseEntity<ResguardoResponse> crearResguardo(
            @RequestBody ResguardoRequest request, 
            Principal principal) {
        ResguardoResponse response = resguardoService.crearResguardo(request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 2. Obtener todo el historial de resguardos (Trazabilidad)
    @GetMapping
    public ResponseEntity<List<ResguardoResponse>> listarTodos() {
        return ResponseEntity.ok(resguardoService.listarTodos());
    }

    // 3. Modificar el estado a DEVUELTO cuando regresen el equipo
    @PutMapping("/{id}/devolucion")
    public ResponseEntity<ResguardoResponse> devolverEquipo(@PathVariable Long id) {
        try {
            ResguardoResponse response = resguardoService.devolverEquipo(id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/test-alertas")
    public ResponseEntity<Map<String, Object>> probarAlertas() {
        try {
            resguardoService.verificarVencimientos();
            return ResponseEntity.ok(Map.of(
                "status", "OK",
                "mensaje", "El escaneo y notificación de vencimientos se ejecutó correctamente."
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "ERROR",
                "mensaje", "Fallo al ejecutar verificación: " + e.getMessage()
            ));
        }
    }
}