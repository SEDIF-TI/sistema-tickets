package com.sedif.sistema_tickets.core.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test")
public class TestResource {

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        // Obtenemos los datos del usuario que el filtro JWT guardó en la memoria
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String correoUsuario = authentication.getName();

        return ResponseEntity.ok("¡Acceso concedido! El filtro de seguridad funciona perfectamente. Usuario logueado: " + correoUsuario);
    }
}