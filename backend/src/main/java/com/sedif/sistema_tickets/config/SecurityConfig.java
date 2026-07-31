package com.sedif.sistema_tickets.config;
import com.sedif.sistema_tickets.core.auth.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;

import java.util.List;
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthFilter;
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Habilita CORS utilizando la configuración definida abajo
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(ex -> ex
                // Manejo cuando un usuario autenticado intenta acceder a un recurso sin los permisos necesarios
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.getWriter().write("Acceso denegado: Se requiere rol de administrador.");
                })
                // Manejo cuando el token es inválido o no se ha proporcionado autenticación
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(403);
                    response.getWriter().write("Acceso denegado: Autenticación requerida.");
                })
            )
            .authorizeHttpRequests(auth -> auth
                // Tus rutas públicas (login, ws, etc)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/ws-tickets/**").permitAll()
                .requestMatchers("/error").permitAll()
                // Tu regla para cambiar contraseña
                .requestMatchers("/api/v1/admin/usuarios/password", "/api/usuarios/password").authenticated()
                // --- ¡ESTA ES LA LÍNEA QUE DEBES AGREGAR PARA SOLUCIONAR EL ERROR 403! ---
                .requestMatchers("/api/v1/tickets/**").hasAnyAuthority("EMPLEADO", "ROLE_EMPLEADO", "SOPORTE", "ROLE_SOPORTE", "ADMINISTRADOR", "ROLE_ADMINISTRADOR")
                // Tu regla de administradores (siempre va al final de las reglas específicas)
                .requestMatchers("/api/v1/admin/**").hasAnyAuthority("ADMINISTRADOR", "ROLE_ADMINISTRADOR")
                // Cualquier otra petición debe estar autenticada
                .anyRequest().authenticated()
            )
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        config.setAllowCredentials(true);
        
        
        config.setAllowedOrigins(List.of("http://localhost", "http://localhost:5173", "http://localhost:5174", "http://localhost:4173")); 
        
        config.setAllowedHeaders(List.of("*"));
        
        // 2. CAMBIO CLAVE: Agregamos "PATCH" para que no se rompa el interruptor de Telegram
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        
        source.registerCorsConfiguration("/**", config);
        return source;
    }
} 