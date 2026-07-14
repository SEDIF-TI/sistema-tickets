package com.sedif.sistema_tickets.core.actividad;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActividadExtraRepository extends JpaRepository<ActividadExtra, Long> {
    
    // Admin
    List<ActividadExtra> findByFechaActividadBetween(LocalDateTime inicio, LocalDateTime fin);

    // Soporte / Desarrollo - ¡Con guion bajo!
    List<ActividadExtra> findByUsuario_IdAndFechaActividadBetween(Long usuarioId, LocalDateTime inicio, LocalDateTime fin);
}