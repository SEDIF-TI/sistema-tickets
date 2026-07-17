package com.sedif.sistema_tickets.core.usuarios;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.sedif.sistema_tickets.core.usuarios.dto.CambioPasswordRequest;

import java.security.Principal;
import java.util.List;

/**
 * Controlador REST que expone los endpoints para la gestión de usuarios.
 */
@RestController
// CAMBIO 1: Ruta exacta que espera tu frontend y tu SecurityConfig
@RequestMapping("/api/v1/admin/usuarios") 
@RequiredArgsConstructor 
public class UsuarioResource {

    private final UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<?> crearUsuario(@RequestBody UsuarioRequest request) {
        try {
            UsuarioResponse response = usuarioService.crearUsuario(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.listarUsuarios());
    }

    // --- CAMBIO 2: Endpoints faltantes para los botones de React ---

    @PutMapping("/{id}/reset-password")
    public ResponseEntity<?> resetearPassword(@PathVariable Long id) {
        try {
            UsuarioResponse response = usuarioService.resetearPassword(id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/toggle-estado")
    public ResponseEntity<?> alternarEstado(@PathVariable Long id) {
        try {
            UsuarioResponse response = usuarioService.alternarEstadoUsuario(id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- Fin de los endpoints nuevos ---

    @PatchMapping("/{id}/disponibilidad")
    public ResponseEntity<?> actualizarDisponibilidad(
            @PathVariable Long id, 
            @RequestBody DisponibilidadRequest request) {
        try {
            UsuarioResponse response = usuarioService.actualizarDisponibilidad(id, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> inactivarUsuario(@PathVariable Long id) {
        try {
            UsuarioResponse response = usuarioService.inactivarUsuario(id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarUsuario(
            @PathVariable Long id, 
            @RequestBody ActualizarUsuarioRequest request) {
        try {
            UsuarioResponse response = usuarioService.actualizarUsuario(id, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ADVERTENCIA: Este endpoint ahora vive en /api/v1/admin/usuarios/password
    @PutMapping("/password")
    public ResponseEntity<String> cambiarPassword(@RequestBody CambioPasswordRequest request, Principal principal) {
        usuarioService.actualizarPassword(principal.getName(), request.nuevaPassword());
        return ResponseEntity.ok("Contraseña actualizada correctamente.");
    }

    // --- NUEVO ENDPOINT PARA REACT: OBTENER SOLO USUARIOS DE SOPORTE ---
    @GetMapping("/soporte")
    public ResponseEntity<List<UsuarioResponse>> listarUsuariosSoporte() {
        // Llama a tu servicio para filtrar solo los de rol "SOPORTE"
        return ResponseEntity.ok(usuarioService.listarSoporte());
    }
}