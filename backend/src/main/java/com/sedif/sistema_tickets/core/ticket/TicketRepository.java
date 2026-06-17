package com.sedif.sistema_tickets.core.ticket;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // Simplificamos: Spring entenderá la jerarquía usuarioArea -> area -> id
    List<Ticket> findByUsuarioAreaAreaId(Long areaId);

    // Para el filtro de soporte
    List<Ticket> findByUsuarioSoporteId(Long soporteId);

    // Corregimos este para que coincida con el uso en TicketService
    long countByUsuarioSoporteAndEstatusNombre(Usuario usuarioSoporte, String nombreEstatus);

    // El filtro ordenado por fecha
    List<Ticket> findByUsuarioAreaAreaIdOrderByFechaCreacionDesc(Long areaId);

    // NUEVO: Trae solo los tickets asignados a un técnico específico (para su propia vista)
    List<Ticket> findByUsuarioSoporte_IdOrderByFechaCreacionDesc(Long soporteId);

    // Busca tickets navegando: Ticket -> Usuario (usuarioArea) -> Area -> Id
    List<Ticket> findByUsuarioArea_Area_IdOrderByFechaCreacionDesc(Long areaId);
    @Query("SELECT t.usuarioArea.area.nombre, COUNT(t) FROM Ticket t GROUP BY t.usuarioArea.area.nombre")
    List<Object[]> contarTicketsPorArea();
}