package com.sedif.sistema_tickets.core.auth;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Extraer el encabezado Authorization
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String identificador;

        // Si no hay encabezado o no empieza con "Bearer ", lo dejamos pasar (SecurityConfig lo bloqueará después si es ruta protegida)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
        filterChain.doFilter(request, response);
        return;
    }

        // 2. Extraer el token (quitamos la palabra "Bearer " que ocupa 7 caracteres)
        jwt = authHeader.substring(7);
        identificador = jwtService.extraerIdentificador(jwt);

        // 3. Si hay un usuario en el token y aún no está autenticado en este hilo
                if (identificador != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    String correoNormalizado = identificador.toLowerCase().trim();
                    
                    // Buscamos al usuario en la base de datos
                    Usuario usuario = usuarioRepository.findByCorreoOrUsername(correoNormalizado, correoNormalizado)
                            .orElse(null);
                    
                    // Validamos: que exista, que esté activo y que el JWT sea auténtico
                    if (usuario != null && Boolean.TRUE.equals(usuario.getActivo()) && jwtService.isTokenValido(jwt)) {
                        
                        // CAMBIO AQUÍ: Pasamos el usuario.getCorreo() (String) en lugar de 'usuario' (objeto)
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                usuario.getCorreo(), 
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getNombre()))
                        );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    } else {
                        System.err.println("DEBUG: El usuario fue encontrado pero falló la validación (Activo: " 
                            + (usuario != null ? usuario.getActivo() : "N/A") + " o Token inválido)");
                    }
                }
        
        // 4. Continuar con la cadena de filtros
        filterChain.doFilter(request, response);
    }
}