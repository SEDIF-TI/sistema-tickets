package com.sedif.sistema_tickets.util.audit;

import org.springframework.data.domain.AuditorAware;
import java.util.Optional;

/**
 * Proveedor del autor que la auditoria de JPA escribe en {@code creadoPor} y
 * {@code modificadoPor}.
 *
 * <p>Se consulta en cada alta y en cada modificacion de una entidad
 * {@link Auditable}, dentro del flush de la sesion de persistencia. Devuelve un
 * identificador fijo del sistema, de modo que las columnas de autoria registran
 * la procedencia de la escritura, no el usuario concreto que la origino.</p>
 *
 * <p>Devolver siempre un valor presente es lo que garantiza que
 * {@code s_creado_por} nunca quede nulo: un {@link Optional} vacio dejaria la
 * columna sin rellenar.</p>
 */
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.of("SISTEMA_USER");
    }
}