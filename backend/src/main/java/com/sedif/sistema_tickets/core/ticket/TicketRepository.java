package com.sedif.sistema_tickets.core.ticket;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.util.enums.EstadoTicket;
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

    // Los tres metodos aceptan ademas un texto libre y un estatus, ambos
    // opcionales (null = sin filtrar). Antes la busqueda se hacia en el
    // navegador sobre la pagina ya descargada, de modo que "buscar" solo
    // miraba 10 de los miles de tickets existentes.

    /** Vision GLOBAL: todos los tickets. Exclusivo de ADMINISTRADOR. */
    @EntityGraph(attributePaths = {"usuarioArea", "usuarioArea.area", "usuarioSoporte"})
    @Query("""
            SELECT t FROM Ticket t
            WHERE (:busqueda IS NULL
                   OR LOWER(t.titulo) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                   OR LOWER(t.usuarioArea.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                   OR LOWER(t.usuarioArea.area.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                   OR CAST(t.id AS string) LIKE CONCAT('%', :busqueda, '%'))
              AND (:estado IS NULL OR t.estado = :estado)
            """)
    Page<Ticket> buscarTodosPaginado(@Param("busqueda") String busqueda,
                                     @Param("estado") EstadoTicket estado,
                                     Pageable pageable);

    /** Vision AREA: tickets levantados por personal del area indicada. */
    @EntityGraph(attributePaths = {"usuarioArea", "usuarioArea.area", "usuarioSoporte"})
    @Query("""
            SELECT t FROM Ticket t
            WHERE t.usuarioArea.area.id = :areaId
              AND (:busqueda IS NULL
                   OR LOWER(t.titulo) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                   OR LOWER(t.usuarioArea.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                   OR CAST(t.id AS string) LIKE CONCAT('%', :busqueda, '%'))
              AND (:estado IS NULL OR t.estado = :estado)
            """)
    Page<Ticket> buscarPorAreaPaginado(@Param("areaId") Long areaId,
                                       @Param("busqueda") String busqueda,
                                       @Param("estado") EstadoTicket estado,
                                       Pageable pageable);

    /**
     * Vision PERSONAL: tickets asignados al tecnico MAS los que el mismo
     * levanto. La version anterior solo miraba la asignacion, asi que un
     * tecnico no veia sus propios reportes.
     */
    @EntityGraph(attributePaths = {"usuarioArea", "usuarioArea.area", "usuarioSoporte"})
    @Query("""
            SELECT t FROM Ticket t
            WHERE (t.usuarioSoporte.id = :usuarioId OR t.usuarioArea.id = :usuarioId)
              AND (:busqueda IS NULL
                   OR LOWER(t.titulo) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                   OR LOWER(t.usuarioArea.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                   OR LOWER(t.usuarioArea.area.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                   OR CAST(t.id AS string) LIKE CONCAT('%', :busqueda, '%'))
              AND (:estado IS NULL OR t.estado = :estado)
            """)
    Page<Ticket> buscarPorSoporteOCreadorPaginado(@Param("usuarioId") Long usuarioId,
                                                  @Param("busqueda") String busqueda,
                                                  @Param("estado") EstadoTicket estado,
                                                  Pageable pageable);

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

    long countByUsuarioSoporteAndEstado(Usuario usuarioSoporte, EstadoTicket estado);

    // =====================================================================
    // METRICAS DEL DASHBOARD
    // =====================================================================

    long countByEstado(EstadoTicket estado);

    long countByEstadoNot(EstadoTicket estado);

    @Query("SELECT t.usuarioArea.area.nombre, COUNT(t) FROM Ticket t GROUP BY t.usuarioArea.area.nombre")
    List<Object[]> contarTicketsPorArea();

    @Query("SELECT t.estado, COUNT(t) FROM Ticket t GROUP BY t.estado")
    List<Object[]> contarPorEstatus();

    @Query("SELECT t.usuarioSoporte.nombre, COUNT(t) FROM Ticket t WHERE t.usuarioSoporte IS NOT NULL GROUP BY t.usuarioSoporte.nombre")
    List<Object[]> contarPorIngeniero();

    @Query("SELECT CAST(t.fechaCreacion AS date), COUNT(t) FROM Ticket t GROUP BY CAST(t.fechaCreacion AS date) ORDER BY CAST(t.fechaCreacion AS date) ASC")
    List<Object[]> contarPorFecha();

    @Query("SELECT t.prioridad, COUNT(t) FROM Ticket t GROUP BY t.prioridad")
    List<Object[]> contarPorPrioridad();
}
