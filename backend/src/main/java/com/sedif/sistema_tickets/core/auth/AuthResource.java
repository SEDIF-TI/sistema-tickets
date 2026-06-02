package com.sedif.sistema_tickets.core.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthResource {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseRecord> login(@RequestBody LoginRequestRecord request) {
        AuthResponseRecord response = authService.iniciarSesion(request);
        return ResponseEntity.ok(response);
    }
}