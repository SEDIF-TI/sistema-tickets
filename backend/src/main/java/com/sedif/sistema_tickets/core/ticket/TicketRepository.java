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

    // ... lo que ya tienes ...

    // NUEVO: Contar tickets según el nombre de su estatus (ej. "RESUELTO", "ABIERTO")
    long countByEstatusNombreIgnoreCase(String nombreEstatus);

    // NUEVO: Contar tickets que NO estén resueltos (pendientes)
    long countByEstatusNombreNotIgnoreCase(String nombreEstatus);

    @Query("SELECT t.estatus.nombre, COUNT(t) FROM Ticket t GROUP BY t.estatus.nombre")
    List<Object[]> contarPorEstatus();

    @Query("SELECT t.usuarioSoporte.nombre, COUNT(t) FROM Ticket t WHERE t.usuarioSoporte IS NOT NULL GROUP BY t.usuarioSoporte.nombre")
    List<Object[]> contarPorIngeniero();

    @Query("SELECT CAST(t.fechaCreacion AS date), COUNT(t) FROM Ticket t GROUP BY CAST(t.fechaCreacion AS date) ORDER BY CAST(t.fechaCreacion AS date) ASC")
    List<Object[]> contarPorFecha();
    
    @Query("SELECT t.prioridad, COUNT(t) FROM Ticket t GROUP BY t.prioridad")
    List<Object[]> contarPorPrioridad();
}