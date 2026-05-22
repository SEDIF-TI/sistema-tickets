package com.sedif.sistema_tickets.util.audit;

import org.springframework.data.domain.AuditorAware;
import java.util.Optional;

public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        // Temporalmente asignamos un usuario estático del sistema.
        // Más adelante, se capturará dinámicamente al usuario logueado en la aplicación.
        return Optional.of("SISTEMA_USER");
    }
}