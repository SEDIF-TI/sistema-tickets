package com.sedif.sistema_tickets.core.ticket;

import com.sedif.sistema_tickets.core.estatusticket.Estatus;
import com.sedif.sistema_tickets.core.usuarios.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // <-- Importación necesaria para el @Query

import java.time.LocalDateTime;
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

    // =======================================================================
    // CONSULTAS NATIVAS INFALIBLES (Van directo a la Base de Datos)
    // =======================================================================

    // 1. Consulta para el ADMIN (Trae todo)
    @Query(value = "SELECT * FROM ticket WHERE d_fecha_fin BETWEEN :inicio AND :fin AND fn_estadoticket_id = :estatusId", nativeQuery = true)
    List<Ticket> buscarTicketsGlobales(
        @Param("inicio") LocalDateTime inicio, 
        @Param("fin") LocalDateTime fin, 
        @Param("estatusId") Long estatusId
    );

    // 2. Consulta para SOPORTE (Filtra por ingeniero exacto)
    @Query(value = "SELECT * FROM ticket WHERE d_fecha_fin BETWEEN :inicio AND :fin AND fn_estadoticket_id = :estatusId AND fn_usuario_soporte_id = :soporteId", nativeQuery = true)
    List<Ticket> buscarTicketsDelTecnico(
        @Param("inicio") LocalDateTime inicio, 
        @Param("fin") LocalDateTime fin, 
        @Param("estatusId") Long estatusId, 
        @Param("soporteId") Long soporteId
    );
}