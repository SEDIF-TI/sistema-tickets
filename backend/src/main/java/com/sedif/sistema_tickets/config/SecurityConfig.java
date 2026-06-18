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
                // Permite las peticiones de verificación de CORS (Pre-flight) desde React
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() 
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/ws-tickets/**").permitAll()
                .requestMatchers("/error").permitAll()
                
                // --- LA EXCEPCIÓN: Cualquier usuario logueado puede actualizar su clave ---
                .requestMatchers("/api/v1/admin/usuarios/password", "/api/usuarios/password").authenticated()
                
                // --- La regla de administradores (DEBE IR DESPUÉS DE LA EXCEPCIÓN) ---
                .requestMatchers("/api/v1/admin/**").hasAnyAuthority("ADMINISTRADOR", "ROLE_ADMINISTRADOR")
                
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
        config.setAllowedOriginPatterns(List.of("*")); 
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}