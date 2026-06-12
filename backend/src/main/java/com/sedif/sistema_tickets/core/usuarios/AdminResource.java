package com.sedif.sistema_tickets.core.usuarios;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/usuarios")
@RequiredArgsConstructor
public class AdminResource {

    private final UsuarioService usuarioService;

    // 1. Listar todos los usuarios para la tabla de administración
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> obtenerTodosLosUsuarios() {
        return ResponseEntity.ok(usuarioService.obtenerTodosLosUsuarios());
    }

    // 2. Crear nuevo usuario
    @PostMapping
    public ResponseEntity<UsuarioResponse> crearUsuario(@RequestBody UsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.crearUsuario(request));
    }

    // 3. Resetear contraseña (generar nueva temporal y forzar cambio)
    @PutMapping("/{id}/reset-password")
    public ResponseEntity<UsuarioResponse> resetearPassword(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.resetearPassword(id));
    }

    // 4. Alternar estado activo/inactivo (bloqueo de cuentas)
    @PutMapping("/{id}/toggle-estado")
    public ResponseEntity<UsuarioResponse> toggleEstado(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.alternarEstadoUsuario(id));
    }
}