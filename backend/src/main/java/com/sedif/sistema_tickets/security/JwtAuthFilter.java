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
 * <p>Extiende {@link OncePerRequestFilter}, de modo que se ejecuta una sola vez
 * por peticion aunque haya reenvios internos. Espera un encabezado con el
 * formato {@code Bearer <token>}; el token se valida criptograficamente y solo
 * entonces se resuelve el usuario contra la base de datos. La identidad
 * resultante queda en el {@link SecurityContextHolder} con una unica autoridad
 * con formato {@code ROLE_<ROL>}, que es la que evaluan los
 * {@code @PreAuthorize} de los controladores.</p>
 *
 * <p>Una peticion sin token, con token invalido o de una cuenta desactivada
 * continua la cadena sin autenticar: quien decide si eso basta para la ruta
 * solicitada es {@code SecurityConfig}, que respondera 401 si la exige.</p>
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

        // Sin encabezado o sin el prefijo Bearer no hay nada que resolver: la
        // peticion sigue sin autenticar y SecurityConfig decide si la admite.
        if (encabezado == null || !encabezado.startsWith(PREFIJO)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Una autenticacion ya presente en el contexto tiene prioridad: no se
        // sobrescribe con la del token.
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = encabezado.substring(PREFIJO.length()).trim();

        // Verificacion de firma y vigencia. Es una operacion en memoria y va
        // primero: descarta los tokens invalidos sin tocar la base de datos, de
        // forma que enviar tokens basura no genera carga de consultas.
        final String identificador = tokenProvider.extraerIdentificador(token);
        if (identificador == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Con un token criptograficamente valido se consulta la cuenta, porque
        // el token pudo emitirse antes de que se desactivara o se le retirara
        // el rol: la firma acredita el origen, no el estado actual del usuario.
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

            // El principal es el correo, que es lo que SecurityUtil y la
            // auditoria esperan encontrar; el username es solo el respaldo.
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
     * Busca al usuario por correo o por nombre de usuario, con un segundo
     * intento en minusculas y sin espacios para tolerar diferencias de
     * mayusculas entre lo firmado en el token y lo almacenado.
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
     * Excluye del filtro las rutas publicas: login, handshake de WebSocket,
     * sondeo de salud y preflight CORS. En ninguna de ellas hay token que
     * procesar, y las peticiones OPTIONS las emite el navegador sin
     * credenciales.
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
