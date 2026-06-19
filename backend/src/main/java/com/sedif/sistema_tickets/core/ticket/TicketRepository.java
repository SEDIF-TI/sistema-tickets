package com.sedif.sistema_tickets.core.ticket;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // Simplificamos: Spring entenderá la jerarquía usuarioArea -> area -> id
    List<Ticket> findByUsuarioAreaAreaId(Long areaId);

    // Para el filtro de soporte
    List<Ticket> findByUsuarioSoporteId(Long soporteId);

    // Corregimos este para que coincida con el uso en TicketService
    long countByUsuarioSoporteAndEstatusNombre(Usuario usuarioSoporte, String nombreEstatus);

    // El filtro ordenado por fecha
    List<Ticket> findByUsuarioAreaAreaIdOrderByFechaCreacionDesc(Long areaId);

    @Query("SELECT t.usuarioArea.area.nombre, COUNT(t) FROM Ticket t GROUP BY t.usuarioArea.area.nombre")
    List<Object[]> contarTicketsPorArea();

    // ... lo que ya tienes ...

    // NUEVO: Contar tickets según el nombre de su estatus (ej. "RESUELTO", "ABIERTO")
    long countByEstatusNombreIgnoreCase(String nombreEstatus);

    // NUEVO: Contar tickets que NO estén resueltos (pendientes)
    long countByEstatusNombreNotIgnoreCase(String nombreEstatus);
}