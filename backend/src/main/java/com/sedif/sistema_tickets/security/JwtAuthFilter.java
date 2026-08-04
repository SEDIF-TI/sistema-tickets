package com.sedif.sistema_tickets.security;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Autentica cada peticion a partir del JWT del encabezado {@code Authorization}.
 *
 * <p>Reemplaza a {@code core.auth.JwtAuthenticationFilter}. Cambios respecto a
 * aquella version:</p>
 * <ul>
 *   <li>La firma del token se verifica ANTES de consultar la base de datos.
 *       Antes se hacia al reves, de modo que un atacante enviando tokens
 *       basura provocaba consultas a la BD en cada peticion.</li>
 *   <li>Se elimino el volcado por {@code System.out} de correo, nombre y rol
 *       en cada peticion, que filtraba datos personales a los logs.</li>
 *   <li>La autoridad se normaliza a {@code ROLE_<ROL>} en un unico punto.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private static final String ENCABEZADO = "Authorization";
    private static final String PREFIJO = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String encabezado = request.getHeader(ENCABEZADO);

        // Sin token: la peticion sigue sin autenticar. Si la ruta esta
        // protegida, SecurityConfig la rechazara mas adelante con un 401.
        if (encabezado == null || !encabezado.startsWith(PREFIJO)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Si ya hay una autenticacion en el contexto, no se vuelve a resolver.
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = encabezado.substring(PREFIJO.length()).trim();

        // PASO 1 — Verificar la firma y la expiracion. Es una operacion en
        // memoria: descarta los tokens invalidos sin tocar la base de datos.
        final String identificador = tokenProvider.extraerIdentificador(token);
        if (identificador == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // PASO 2 — Solo con un token criptograficamente valido se consulta la BD,
        // para confirmar que la cuenta sigue existiendo y activa.
        buscarUsuario(identificador).ifPresent(usuario -> {
            if (!Boolean.TRUE.equals(usuario.getActivo())) {
                log.debug("Se rechazo un token de una cuenta desactivada.");
                return;
            }
            if (usuario.getRol() == null) {
                log.warn("Usuario id={} sin rol asignado: no se puede autorizar.", usuario.getId());
                return;
            }

            String autoridad = "ROLE_" + usuario.getRol().getNombre().toUpperCase();

            // El principal es el correo; se recurre al username solo si falta.
            String principal = (usuario.getCorreo() != null && !usuario.getCorreo().isBlank())
                    ? usuario.getCorreo()
                    : usuario.getUsername();

            var authToken = new UsernamePasswordAuthenticationToken(
                    principal, null, List.of(new SimpleGrantedAuthority(autoridad)));
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authToken);
        });

        filterChain.doFilter(request, response);
    }

    /**
     * Busca por correo o username. Se reintenta en minusculas para tolerar
     * diferencias de mayusculas en los datos historicos.
     */
    private Optional<Usuario> buscarUsuario(String identificador) {
        Optional<Usuario> usuario =
                usuarioRepository.findByCorreoOrUsername(identificador, identificador);
        if (usuario.isPresent()) {
            return usuario;
        }
        String normalizado = identificador.toLowerCase().trim();
        return usuarioRepository.findByCorreoOrUsername(normalizado, normalizado);
    }

    /**
     * El filtro no se ejecuta en rutas publicas: no hay token que procesar y
     * asi se evita trabajo innecesario en login y handshake de WebSocket.
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String ruta = request.getServletPath();
        return ruta.startsWith("/api/v1/auth/")
                || ruta.startsWith("/ws-tickets")
                || ruta.equals("/api/salud")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }
}
