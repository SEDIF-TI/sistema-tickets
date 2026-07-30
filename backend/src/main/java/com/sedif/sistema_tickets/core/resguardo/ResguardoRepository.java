package com.sedif.sistema_tickets.core.resguardo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ResguardoRepository extends JpaRepository<Resguardo, Long> {
    List<Resguardo> findAllByOrderByFechaCreacionDesc();
    List<Resguardo> findByFechaVencimientoBeforeAndEstado(LocalDateTime fecha, EstadoResguardo estado);
}