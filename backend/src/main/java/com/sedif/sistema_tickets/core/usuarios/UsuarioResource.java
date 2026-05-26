package com.sedif.sistema_tickets.core.usuarios;

<<<<<<< HEAD
=======
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
>>>>>>> modulo_areas
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

<<<<<<< HEAD
@RestController
@RequestMapping("/api/usuarios")
=======
/**
 * Controlador REST que expone los endpoints para la gestión de usuarios.
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor // Lombok: Genera automáticamente el constructor para inyectar UsuarioService
>>>>>>> modulo_areas
public class UsuarioResource {

    private final UsuarioService usuarioService;

<<<<<<< HEAD
    // Inyección de dependencias
    public UsuarioResource(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // Endpoint para CREAR un usuario (El que estás probando en Postman)
    @PostMapping
    public ResponseEntity<UsuarioResponse> registrarUsuario(@RequestBody UsuarioRequest request) {
        UsuarioResponse usuarioCreado = usuarioService.crearUsuario(request);
        return ResponseEntity.ok(usuarioCreado);
    }

    // Endpoint para LISTAR los usuarios
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> obtenerUsuarios() {
=======
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
>>>>>>> modulo_areas
        return ResponseEntity.ok(usuarioService.listarUsuarios());
    }
}