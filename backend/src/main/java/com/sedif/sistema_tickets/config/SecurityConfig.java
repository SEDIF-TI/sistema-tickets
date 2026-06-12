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
import org.springframework.web.filter.CorsFilter; // Importante: usa este
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
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(request -> {
             var corsConfig = new CorsConfiguration();
             corsConfig.setAllowedOriginPatterns(List.of("*"));
             corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
             corsConfig.setAllowedHeaders(List.of("*"));
             corsConfig.setAllowCredentials(true);
             return corsConfig;
        }))
        .authorizeHttpRequests(auth -> auth
            // 1. Rutas públicas
            .requestMatchers("/api/v1/auth/**").permitAll()
            .requestMatchers("/ws-tickets/**").permitAll()
            
            // 2. RUTA PROTEGIDA: Solo administradores
            // Nota: Spring Security añade el prefijo "ROLE_" automáticamente si usas hasRole()
            .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
            
            // 3. El resto de rutas requieren estar autenticado (cualquier rol)
            .anyRequest().authenticated()
        )
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
}

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}