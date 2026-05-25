package com.sedif.sistema_tickets.core.usuarios;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioResource {

    private final UsuarioService usuarioService;

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
        return ResponseEntity.ok(usuarioService.listarUsuarios());
    }
}