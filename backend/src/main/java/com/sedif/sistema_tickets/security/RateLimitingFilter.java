package com.sedif.sistema_tickets.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita los intentos contra los endpoints de autenticacion.
 *
 * <p>Sin este filtro, {@code /api/v1/auth/login} acepta intentos ilimitados,
 * lo que permite fuerza bruta sobre las contrasenas. El riesgo se agrava
 * porque las contrasenas temporales son cortas por naturaleza.</p>
 *
 * <p>Se aplica una cubeta por combinacion de IP y ruta, de modo que agotar el
 * limite de login no bloquea otros endpoints. Al superarlo se responde
 * {@code 429 Too Many Requests} en JSON, con el mismo formato de error que
 * usa el resto de la API.</p>
 *
 * <p><b>Nota sobre despliegue:</b> detras de un reverse proxy la IP directa es
 * la del proxy. Por eso se lee {@code X-Forwarded-For} y en produccion se
 * exige {@code server.forward-headers-strategy=framework}.</p>
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    /**
     * Rutas vigiladas. El estandar contempla ademas /auth/identificar, que
     * este proyecto no implementa: no tiene flujo de identificacion previa.
     */
    private static final Set<String> RUTAS_PROTEGIDAS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/registro"
    );

    /**
     * Una cubeta por clave "ip|ruta". Se usa ConcurrentHashMap porque el
     * filtro se ejecuta en paralelo desde varios hilos del contenedor.
     */
    private final Map<String, Bucket> cubetas = new ConcurrentHashMap<>();

    private final boolean habilitado;
    private final long capacidad;
    private final Duration periodoRecarga;

    public RateLimitingFilter(
            @Value("${app.rate-limit.enabled:true}") boolean habilitado,
            @Value("${app.rate-limit.capacity:5}") long capacidad,
            @Value("${app.rate-limit.refill-minutes:1}") long minutosRecarga) {
        this.habilitado = habilitado;
        this.capacidad = capacidad;
        this.periodoRecarga = Duration.ofMinutes(minutosRecarga);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String clave = obtenerIpCliente(request) + "|" + request.getServletPath();
        Bucket cubeta = cubetas.computeIfAbsent(clave, k -> crearCubeta());

        if (cubeta.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Se registra la ruta, no la IP completa, para no acumular datos
        // personales en los logs mas alla de lo necesario.
        log.warn("Limite de peticiones superado en {}", request.getServletPath());
        responderDemasiadasPeticiones(response);
    }

    /** Solo se filtran las rutas de autenticacion; el resto no paga coste. */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        if (!habilitado) {
            return true;
        }
        // Las preflight CORS no son intentos de autenticacion.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        return !RUTAS_PROTEGIDAS.contains(request.getServletPath());
    }

    private Bucket crearCubeta() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacidad)
                        .refillIntervally(capacidad, periodoRecarga)
                        .build())
                .build();
    }

    /**
     * Obtiene la IP real del cliente. {@code X-Forwarded-For} puede traer una
     * cadena de proxies: el primer elemento es el cliente original.
     */
    private String obtenerIpCliente(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    /** Responde 429 con el mismo envoltorio JSON que usa el resto de la API. */
    private void responderDemasiadasPeticiones(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"success":false,"message":"Demasiados intentos. Espere unos minutos e intente de nuevo.","data":null}\
                """);
    }
}
