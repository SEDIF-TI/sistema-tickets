package com.sedif.sistema_tickets.security.auth;

import com.sedif.sistema_tickets.exception.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints publicos de autenticacion.
 *
 * <p>Es la unica ruta accesible sin token, y por eso esta vigilada por
 * {@code RateLimitingFilter} frente a ataques de fuerza bruta.</p>
 *
 * <p><b>El endpoint {@code POST /registro} fue eliminado.</b> Era publico y
 * tomaba el {@code rolId} del cuerpo de la peticion, de modo que cualquiera
 * podia crearse una cuenta de administrador sin autenticarse. El alta de
 * usuarios se hace desde {@code UsuarioResource}, que exige rol
 * ADMINISTRADOR.</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** Valida las credenciales y devuelve el JWT junto con los datos de sesion. */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest request) {
        JwtResponse respuesta = authService.iniciarSesion(request);
        return ResponseEntity.ok(ApiResponse.ok(respuesta, respuesta.mensaje()));
    }
}
