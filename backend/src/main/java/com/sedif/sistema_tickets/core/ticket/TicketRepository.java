package com.sedif.sistema_tickets.core.ticket;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    
    // Spring Data JPA sigue resolviendo esto navegando de Ticket -> Estatus -> nombre automáticamente
    long countByUsuarioSoporteAndEstatusNombre(Usuario usuarioSoporte, String nombreEstatus);

    // NUEVO: Trae todos los tickets basándose en el ID del área a la que pertenece el creador
    List<Ticket> findByUsuarioArea_Area_Id(Long areaId);

    // NUEVO: Trae solo los tickets asignados a un técnico específico (para su propia vista)
    List<Ticket> findByUsuarioSoporte_Id(Long soporteId);
}