package com.sedif.sistema_tickets.core.auth;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService; // Inyectamos el nuevo motor de JWT
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public AuthResponseRecord iniciarSesion(LoginRequestRecord request) {
        
        // 1. Validación manual estricta
        if (request == null || request.identificador() == null || request.identificador().isBlank()) {
            throw new IllegalArgumentException("El correo electrónico o usuario es obligatorio.");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria.");
        }

        // 2. Búsqueda por correo o username
        Usuario usuario = usuarioRepository.findByCorreoOrUsername(request.identificador(), request.identificador())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas."));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new IllegalArgumentException("El usuario se encuentra inactivo. Contacte al administrador.");
        }

        // 3. Verificación criptográfica
        if (!passwordEncoder.matches(request.password(), usuario.getPassword())) {
            throw new IllegalArgumentException("Credenciales inválidas.");
        }

        // 4. Generación del JWT en memoria (Sin tocar la base de datos)
        String tokenJwt = jwtService.generarToken(usuario);

        // 5. Retorno
        return new AuthResponseRecord(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getRol().name(),
                tokenJwt,
                "Autenticación exitosa."
        );
    }
}