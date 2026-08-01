package com.sedif.sistema_tickets.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuracion web transversal: CORS.
 *
 * <p>Antes esta configuracion vivia dentro de {@code SecurityConfig} con los
 * origenes {@code localhost:5173} y {@code localhost:5174} escritos a mano, lo
 * que obligaba a recompilar para desplegar en otro dominio. Ahora se leen de
 * {@code app.cors.allowed-origins} (variable de entorno), separados por coma.</p>
 */
@Configuration
public class WebConfig {

    private final List<String> origenesPermitidos;

    public WebConfig(@Value("${app.cors.allowed-origins}") String origenes) {
        this.origenesPermitidos = Arrays.stream(origenes.split(","))
                .map(String::trim)
                .filter(o -> !o.isEmpty())
                .toList();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Lista explicita de origenes. No se usa el comodin "*" porque es
        // incompatible con allowCredentials y permitiria que cualquier sitio
        // llamara a la API desde el navegador de un usuario con sesion activa.
        config.setAllowedOrigins(origenesPermitidos);

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));

        // Necesario para que el navegador pueda leer el encabezado en descargas
        // de PDF (memorandums y requisiciones).
        config.setExposedHeaders(List.of("Content-Disposition"));

        config.setAllowCredentials(true);

        // Cachea la respuesta preflight una hora y reduce peticiones OPTIONS.
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
