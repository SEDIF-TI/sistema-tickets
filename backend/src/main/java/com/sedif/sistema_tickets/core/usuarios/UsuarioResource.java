package com.sedif.sistema_tickets.core.usuarios;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST que expone los endpoints para la gestión de usuarios.
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor // Lombok: Genera automáticamente el constructor para inyectar UsuarioService
public class UsuarioResource {

    private final UsuarioService usuarioService;

    /**
     * Endpoint para crear un usuario.
     * Método HTTP: POST
     * URL: http://localhost:8080/api/usuarios
     */
    @PostMapping
    public ResponseEntity<?> crearUsuario(@RequestBody UsuarioRequest request) {
        try {
            UsuarioResponse response = usuarioService.crearUsuario(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            // Si falla una validación, devolvemos un código 400 (Bad Request) con el mensaje de error
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Endpoint para listar usuarios.
     * Método HTTP: GET
     * URL: http://localhost:8080/api/usuarios
     */
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.listarUsuarios());
    }


    /**
     * Endpoint para cambiar la disponibilidad de un usuario de soporte.
     * Método HTTP: PATCH
     */
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

    /**
     * Endpoint para realizar la baja lógica (inactivación) de un usuario.
     * Método HTTP: DELETE
     * URL: http://localhost:8080/api/usuarios/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> inactivarUsuario(@PathVariable Long id) {
        try {
            UsuarioResponse response = usuarioService.inactivarUsuario(id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}