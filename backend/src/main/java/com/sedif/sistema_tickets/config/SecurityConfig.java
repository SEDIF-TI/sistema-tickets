package com.sedif.sistema_tickets.config;

import com.sedif.sistema_tickets.exception.MessageConstants;
import com.sedif.sistema_tickets.security.JwtAuthFilter;
import com.sedif.sistema_tickets.security.RateLimitingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Cadena de seguridad de la aplicacion.
 *
 * <p>Resumen de las garantias que aplica:</p>
 * <ul>
 *   <li>API sin estado: CSRF desactivado y ninguna sesion de servidor.</li>
 *   <li>Solo {@code /api/v1/auth/**} y el handshake de WebSocket son publicos;
 *       el resto exige autenticacion.</li>
 *   <li>Autorizacion por rol en las anotaciones {@code @PreAuthorize} de cada
 *       controlador, habilitadas con {@link EnableMethodSecurity}. Las reglas
 *       por URL de aqui son una segunda barrera, no la unica.</li>
 *   <li>Cabeceras de seguridad: anti clickjacking, anti MIME sniffing, HSTS
 *       y CSP.</li>
 *   <li>401 cuando falta autenticacion y 403 cuando faltan permisos.</li>
 *   <li>La configuracion CORS se delega en {@code WebConfig}.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    /**
     * BCrypt aplica su propia sal por hash, de modo que dos usuarios con la
     * misma contrasena obtienen hashes distintos.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CORS gestionado por el bean que define WebConfig.
            .cors(cors -> cors.configurationSource(corsConfigurationSource))

            // Sin CSRF: la API es stateless y se autentica por token en un
            // encabezado, no por cookie de sesion enviada por el navegador.
            .csrf(csrf -> csrf.disable())

            // Ninguna sesion en servidor: cada peticion se valida por su JWT.
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .headers(headers -> headers
                    // Impide que la aplicacion se cargue dentro de un iframe
                    // (clickjacking).
                    .frameOptions(frame -> frame.deny())
                    // Impide que el navegador adivine el tipo de contenido.
                    .contentTypeOptions(cto -> {})
                    // Fuerza HTTPS durante un ano, subdominios incluidos.
                    .httpStrictTransportSecurity(hsts -> hsts
                            .includeSubDomains(true)
                            .maxAgeInSeconds(31536000))
                    // La API solo devuelve JSON: se puede bloquear todo lo demas.
                    .contentSecurityPolicy(csp -> csp
                            .policyDirectives("default-src 'self'; frame-ancestors 'none'"))
                    .referrerPolicy(referrer -> referrer.policy(
                            org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
                                    .ReferrerPolicy.SAME_ORIGIN))
            )

            .exceptionHandling(ex -> ex
                    // 401: no hay identidad. El frontend debe redirigir al login.
                    .authenticationEntryPoint((request, response, authException) ->
                            escribirError(response, HttpStatus.UNAUTHORIZED,
                                    MessageConstants.AUTENTICACION_REQUERIDA))
                    // 403: hay identidad, pero sin permisos suficientes.
                    .accessDeniedHandler((request, response, accessDeniedException) ->
                            escribirError(response, HttpStatus.FORBIDDEN,
                                    MessageConstants.ACCESO_DENEGADO))
            )

            .authorizeHttpRequests(auth -> auth
                    // Preflight CORS: el navegador las envia sin credenciales.
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                    // Unicas rutas publicas.
                    .requestMatchers("/api/v1/auth/**").permitAll()
                    .requestMatchers("/ws-tickets/**").permitAll()
                    .requestMatchers("/error").permitAll()
                    // Sondeo de conectividad del frontend. Publico porque la
                    // pantalla de acceso lo consulta antes de que exista
                    // sesion, y no revela nada: responde 204 sin cuerpo.
                    .requestMatchers("/api/salud").permitAll()

                    // Barrera por URL para el area de administracion. La
                    // autorizacion fina vive en los @PreAuthorize de cada
                    // controlador.
                    .requestMatchers("/api/v1/admin/**")
                        .hasAnyAuthority("ROLE_ADMINISTRADOR", "ADMINISTRADOR")

                    // Modulos operativos de TI: resguardos, taller, correos
                    // institucionales, catalogo de equipos y actividades del
                    // plan de trabajo. Son herramientas internas del area, no
                    // del personal general.
                    //
                    // Sin esta barrera quedaban fuera de todo patron protegido
                    // y bastaba con estar autenticado: cualquier EMPLEADO podia
                    // leer y modificar resguardos, reparaciones y las
                    // contrasenas de los correos institucionales.
                    .requestMatchers(
                            "/api/v1/resguardos/**",
                            "/api/taller/**",
                            "/api/correos/**",
                            "/api/v1/equipos/**",
                            "/api/v1/actividades-extras/**")
                        .hasAnyAuthority("ROLE_ADMINISTRADOR", "ADMINISTRADOR",
                                         "ROLE_SOPORTE", "SOPORTE")

                    // Todo lo demas exige, como minimo, estar autenticado.
                    .anyRequest().authenticated()
            )

            // Orden de filtros: primero se limita el ritmo de peticiones, para
            // que un ataque de fuerza bruta ni siquiera llegue a validar tokens.
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Escribe un error con el mismo envoltorio JSON que usa el resto de la API,
     * para que el frontend no tenga que tratar estas respuestas como un caso
     * aparte.
     */
    private void escribirError(jakarta.servlet.http.HttpServletResponse response,
                               HttpStatus estado,
                               String mensaje) throws java.io.IOException {
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"success\":false,\"message\":\"" + mensaje + "\",\"data\":null}");
    }
}
