package com.sedif.sistema_tickets.core.resguardo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ResguardoRepository extends JpaRepository<Resguardo, Long> {

    List<Resguardo> findAllByOrderByFechaCreacionDesc();

    /** Resguardos cuya fecha de vencimiento ya paso y siguen sin devolverse. */
    List<Resguardo> findByFechaVencimientoBeforeAndEstado(LocalDateTime fecha, EstadoResguardo estado);

    /**
     * Resguardos que venceran dentro de una ventana de tiempo.
     *
     * <p>Permite avisar al personal ANTES de que el prestamo caduque, en lugar
     * de reclamarlo cuando ya esta vencido.</p>
     */
    List<Resguardo> findByEstadoAndFechaVencimientoBetween(
            EstadoResguardo estado, LocalDateTime desde, LocalDateTime hasta);

    /** Conteo por estado, para las metricas del panel. */
    long countByEstado(EstadoResguardo estado);
}
