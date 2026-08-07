package com.sedif.sistema_tickets.util.audit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Activa la auditoria automatica de JPA.
 *
 * <p>{@code @EnableJpaAuditing} registra el {@code AuditingEntityListener} que
 * rellena las marcas de tiempo y de autoria de las entidades que heredan de
 * {@link Auditable}, y {@code auditorAwareRef} le indica de que bean obtener el
 * autor de cada operacion.</p>
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditConfig {

    /**
     * Proveedor del autor que se guarda en {@code creadoPor} y
     * {@code modificadoPor}. El nombre del metodo es el del bean al que apunta
     * {@code auditorAwareRef}, de modo que renombrarlo rompe la auditoria.
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return new AuditorAwareImpl();
    }
}