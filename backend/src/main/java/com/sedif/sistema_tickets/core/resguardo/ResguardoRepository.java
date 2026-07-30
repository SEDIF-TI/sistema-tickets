package com.sedif.sistema_tickets.core.resguardo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ResguardoRepository extends JpaRepository<Resguardo, Long> {
    
    // Obtener todos los resguardos ordenados por el más reciente
    List<Resguardo> findAllByOrderByFechaCreacionDesc();
    
    // Buscar los resguardos que vencen hoy o que ya pasaron de la fecha límite y siguen como ENTREGADOS
    List<Resguardo> findByEstadoAndFechaVencimientoLessThanEqual(EstadoResguardo estado, LocalDateTime fechaVencimiento);
}