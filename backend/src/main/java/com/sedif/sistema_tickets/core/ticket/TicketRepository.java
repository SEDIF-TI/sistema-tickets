package com.sedif.sistema_tickets.core.ticket;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // =====================================================================
    // CONSULTAS PAGINADAS
    // ---------------------------------------------------------------------
    // Devuelven Page<Ticket> para que el troceado ocurra en la base de datos
    // (LIMIT/OFFSET) y no en memoria. Antes se usaba findAll() y el servidor
    // cargaba la tabla completa en cada peticion.
    //
    // Se usa @EntityGraph y no "JOIN FETCH": con JOIN FETCH sobre una consulta
    // paginada, Hibernate advierte que aplicara la paginacion EN MEMORIA, que
    // es justo lo que se quiere evitar. El EntityGraph resuelve las relaciones
    // sin romper el LIMIT.
    // =====================================================================

    /** Vision GLOBAL: todos los tickets. Exclusivo de ADMINISTRADOR. */
    @EntityGraph(attributePaths = {"usuarioArea", "usuarioArea.area", "usuarioSoporte", "estatus"})
    @Query("SELECT t FROM Ticket t")
    Page<Ticket> buscarTodosPaginado(Pageable pageable);

    /** Vision AREA: tickets levantados por personal del area indicada. */
    @EntityGraph(attributePaths = {"usuarioArea", "usuarioArea.area", "usuarioSoporte", "estatus"})
    @Query("SELECT t FROM Ticket t WHERE t.usuarioArea.area.id = :areaId")
    Page<Ticket> buscarPorAreaPaginado(@Param("areaId") Long areaId, Pageable pageable);

    /**
     * Vision PERSONAL: tickets asignados al tecnico MAS los que el mismo
     * levanto. La version anterior solo miraba la asignacion, asi que un
     * tecnico no veia sus propios reportes.
     */
    @EntityGraph(attributePaths = {"usuarioArea", "usuarioArea.area", "usuarioSoporte", "estatus"})
    @Query("SELECT t FROM Ticket t WHERE t.usuarioSoporte.id = :usuarioId OR t.usuarioArea.id = :usuarioId")
    Page<Ticket> buscarPorSoporteOCreadorPaginado(@Param("usuarioId") Long usuarioId, Pageable pageable);

    // =====================================================================
    // CONSULTAS SIN PAGINAR
    // Se conservan para el motor de asignacion y los eventos WebSocket.
    // =====================================================================

    List<Ticket> findByUsuarioAreaAreaId(Long areaId);

    List<Ticket> findByUsuarioSoporteId(Long soporteId);

    List<Ticket> findByUsuarioAreaAreaIdOrderByFechaCreacionDesc(Long areaId);

    List<Ticket> findByUsuarioSoporte_IdOrderByFechaCreacionDesc(Long soporteId);

    /** Tickets asignados a un tecnico o creados por el, sin paginar. */
    @Query("SELECT t FROM Ticket t WHERE t.usuarioSoporte.id = :usuarioId OR t.usuarioArea.id = :usuarioId")
    List<Ticket> buscarPorSoporteOCreador(@Param("usuarioId") Long usuarioId);

    long countByUsuarioSoporteAndEstatusNombre(Usuario usuarioSoporte, String nombreEstatus);

    // =====================================================================
    // METRICAS DEL DASHBOARD
    // =====================================================================

    long countByEstatusNombreIgnoreCase(String nombreEstatus);

    long countByEstatusNombreNotIgnoreCase(String nombreEstatus);

    @Query("SELECT t.usuarioArea.area.nombre, COUNT(t) FROM Ticket t GROUP BY t.usuarioArea.area.nombre")
    List<Object[]> contarTicketsPorArea();

    @Query("SELECT t.estatus.nombre, COUNT(t) FROM Ticket t GROUP BY t.estatus.nombre")
    List<Object[]> contarPorEstatus();

    @Query("SELECT t.usuarioSoporte.nombre, COUNT(t) FROM Ticket t WHERE t.usuarioSoporte IS NOT NULL GROUP BY t.usuarioSoporte.nombre")
    List<Object[]> contarPorIngeniero();

    @Query("SELECT CAST(t.fechaCreacion AS date), COUNT(t) FROM Ticket t GROUP BY CAST(t.fechaCreacion AS date) ORDER BY CAST(t.fechaCreacion AS date) ASC")
    List<Object[]> contarPorFecha();

    @Query("SELECT t.prioridad, COUNT(t) FROM Ticket t GROUP BY t.prioridad")
    List<Object[]> contarPorPrioridad();
}
