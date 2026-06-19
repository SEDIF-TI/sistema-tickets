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

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String identificador;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        identificador = jwtService.extraerIdentificador(jwt);

        if (identificador != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            System.out.println("\n====== DEBUG JWT FILTRO ======");
            System.out.println("1. Petición a: " + request.getRequestURI());
            System.out.println("2. Token recibido para identificador: [" + identificador + "]");
            
            // FIX: Buscamos exacto, y si falla, intentamos en minúsculas (previene bugs de Case Sensitivity)
            Usuario usuario = usuarioRepository.findByCorreoOrUsername(identificador, identificador)
                    .orElseGet(() -> usuarioRepository.findByCorreoOrUsername(identificador.toLowerCase().trim(), identificador.toLowerCase().trim()).orElse(null));
            
            if (usuario == null) {
                System.err.println("3. ERROR FATAL: No se encontró el usuario en la Base de Datos.");
            } else {
                System.out.println("3. Usuario BD: " + usuario.getNombre() + " | Rol: " + usuario.getRol().getNombre() + " | Activo: " + usuario.getActivo());
                
                boolean tokenValido = jwtService.isTokenValido(jwt);
                System.out.println("4. Validación del Token: " + (tokenValido ? "VÁLIDO" : "INVÁLIDO"));
                
                if (Boolean.TRUE.equals(usuario.getActivo()) && tokenValido) {
                    String rolFinal = "ROLE_" + usuario.getRol().getNombre().toUpperCase();
                    System.out.println("5. Autoridad inyectada a Spring: " + rolFinal);
                    
                    // FIX: Evita colapsos si el correo está vacío usando el username como respaldo
                    String principal = (usuario.getCorreo() != null && !usuario.getCorreo().isEmpty()) ? usuario.getCorreo() : usuario.getUsername();
                    
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            principal, 
                            null,
                            List.of(new SimpleGrantedAuthority(rolFinal))
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    System.out.println("6. ¡Autenticación finalizada con éxito!");
                } else {
                    System.err.println("5. ERROR: Usuario inactivo o token expirado.");
                }
            }
            System.out.println("==============================\n");
        }
        
        filterChain.doFilter(request, response);
    }
}