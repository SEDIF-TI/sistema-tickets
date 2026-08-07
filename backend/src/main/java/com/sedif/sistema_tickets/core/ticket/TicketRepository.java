package com.sedif.sistema_tickets.core.ticket;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.util.enums.EstadoTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // =====================================================================
    // CONSULTAS PAGINADAS
    // ---------------------------------------------------------------------
    // Devuelven Page<Ticket> para que el troceado ocurra en la base de datos
    // (LIMIT/OFFSET) y no en memoria: el servidor nunca carga la tabla entera.
    //
    // Las relaciones se resuelven con @EntityGraph y no con "JOIN FETCH":
    // sobre una consulta paginada, JOIN FETCH obliga a Hibernate a aplicar la
    // paginacion EN MEMORIA, que es justo lo que se quiere evitar. El
    // EntityGraph trae usuarioArea, su area y usuarioSoporte en la misma
    // consulta sin romper el LIMIT, de modo que el mapeo a DTO no dispara una
    // consulta extra por fila.
    // =====================================================================

    // Los tres metodos aceptan ademas un texto libre y un estado, ambos
    // opcionales (null = sin filtrar), de modo que la busqueda recorre la
    // tabla completa y no solo la pagina que el cliente tiene descargada.
    //
    // El parametro :busqueda llega YA en minusculas y con los comodines '%'
    // puestos por el servicio (ver TicketService.normalizarBusqueda). Se hace
    // asi por dos motivos:
    //
    //   1. PostgreSQL no puede inferir el tipo de un parametro que llega nulo
    //      dentro de LOWER(...): lo trata como bytea y la consulta falla con
    //      "function lower(bytea) does not exist" en cuanto se filtra sin
    //      texto de busqueda. El CAST explicito fija el tipo.
    //   2. Aplicar LOWER solo al lado de la columna permite aprovechar un
    //      indice funcional si en el futuro hiciera falta.
    //
    // El ESCAPE '!' respeta el escapado que hace el servicio sobre '%', '_' y
    // el propio '!', para que esos caracteres se busquen como literales y no
    // como comodines.

    /** Vision GLOBAL: todos los tickets. Exclusivo de ADMINISTRADOR. */
    @EntityGraph(attributePaths = {"usuarioArea", "usuarioArea.area", "usuarioSoporte"})
    @Query("""
            SELECT t FROM Ticket t
            WHERE (CAST(:busqueda AS string) IS NULL
                   OR LOWER(t.titulo) LIKE :busqueda ESCAPE '!'
                   OR LOWER(t.usuarioArea.nombre) LIKE :busqueda ESCAPE '!'
                   OR LOWER(t.usuarioArea.area.nombre) LIKE :busqueda ESCAPE '!'
                   OR CAST(t.id AS string) LIKE :busqueda ESCAPE '!')
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
              AND (CAST(:busqueda AS string) IS NULL
                   OR LOWER(t.titulo) LIKE :busqueda ESCAPE '!'
                   OR LOWER(t.usuarioArea.nombre) LIKE :busqueda ESCAPE '!'
                   OR CAST(t.id AS string) LIKE :busqueda ESCAPE '!')
              AND (:estado IS NULL OR t.estado = :estado)
            """)
    Page<Ticket> buscarPorAreaPaginado(@Param("areaId") Long areaId,
                                       @Param("busqueda") String busqueda,
                                       @Param("estado") EstadoTicket estado,
                                       Pageable pageable);

    /**
     * Vision PERSONAL: tickets asignados al tecnico MAS los que el mismo
     * levanto, de modo que un tecnico ve tanto su carga de trabajo como sus
     * propios reportes.
     */
    @EntityGraph(attributePaths = {"usuarioArea", "usuarioArea.area", "usuarioSoporte"})
    @Query("""
            SELECT t FROM Ticket t
            WHERE (t.usuarioSoporte.id = :usuarioId OR t.usuarioArea.id = :usuarioId)
              AND (CAST(:busqueda AS string) IS NULL
                   OR LOWER(t.titulo) LIKE :busqueda ESCAPE '!'
                   OR LOWER(t.usuarioArea.nombre) LIKE :busqueda ESCAPE '!'
                   OR LOWER(t.usuarioArea.area.nombre) LIKE :busqueda ESCAPE '!'
                   OR CAST(t.id AS string) LIKE :busqueda ESCAPE '!')
              AND (:estado IS NULL OR t.estado = :estado)
            """)
    Page<Ticket> buscarPorSoporteOCreadorPaginado(@Param("usuarioId") Long usuarioId,
                                                  @Param("busqueda") String busqueda,
                                                  @Param("estado") EstadoTicket estado,
                                                  Pageable pageable);

    // =====================================================================
    // CONSULTAS SIN PAGINAR
    // Las usan el motor de asignacion y los eventos WebSocket, que trabajan
    // sobre conjuntos acotados y necesitan la lista completa.
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
    // REPORTE DE ACTIVIDADES
    // El filtro por estado y por fecha de cierre se resuelve en la base: el
    // reporte abarca periodos largos y traer la tabla entera para descartarla
    // en memoria crece con el historial, no con lo que se imprime.
    //
    // El fetch join sobre area, tecnico y su area trae en una sola consulta lo
    // que el generador del documento recorre despues; sin el, cada fila
    // dispararia sus propias consultas al atravesar esas relaciones perezosas.
    // =====================================================================

    /** Tickets cerrados en el periodo, de toda la institucion. */
    @Query("""
            SELECT DISTINCT t FROM Ticket t
            LEFT JOIN FETCH t.usuarioArea ua
            LEFT JOIN FETCH ua.area
            LEFT JOIN FETCH t.usuarioSoporte us
            LEFT JOIN FETCH us.area
            WHERE t.estado = :estado
              AND t.fechaFin BETWEEN :desde AND :hasta
            ORDER BY t.fechaFin ASC
            """)
    List<Ticket> buscarCerradosEnPeriodo(@Param("estado") EstadoTicket estado,
                                         @Param("desde") LocalDateTime desde,
                                         @Param("hasta") LocalDateTime hasta);

    /** Tickets cerrados en el periodo por un tecnico concreto. */
    @Query("""
            SELECT DISTINCT t FROM Ticket t
            LEFT JOIN FETCH t.usuarioArea ua
            LEFT JOIN FETCH ua.area
            LEFT JOIN FETCH t.usuarioSoporte us
            LEFT JOIN FETCH us.area
            WHERE t.estado = :estado
              AND t.usuarioSoporte.id = :tecnicoId
              AND t.fechaFin BETWEEN :desde AND :hasta
            ORDER BY t.fechaFin ASC
            """)
    List<Ticket> buscarCerradosEnPeriodoPorTecnico(@Param("estado") EstadoTicket estado,
                                                   @Param("tecnicoId") Long tecnicoId,
                                                   @Param("desde") LocalDateTime desde,
                                                   @Param("hasta") LocalDateTime hasta);

    // =====================================================================
    // METRICAS DEL DASHBOARD
    // Agregados que se calculan en la base con GROUP BY: cada uno devuelve
    // Object[] con la clave de agrupacion en la posicion 0 y el conteo en la
    // 1, que es lo que el servicio traduce a los datos de cada grafica.
    // =====================================================================

    long countByEstado(EstadoTicket estado);

    long countByEstadoNot(EstadoTicket estado);

    @Query("SELECT t.usuarioArea.area.nombre, COUNT(t) FROM Ticket t GROUP BY t.usuarioArea.area.nombre")
    List<Object[]> contarTicketsPorArea();

    @Query("SELECT t.estado, COUNT(t) FROM Ticket t GROUP BY t.estado")
    List<Object[]> contarPorEstatus();

    /** Carga por tecnico. Los tickets sin asignar quedan fuera del agrupado. */
    @Query("SELECT t.usuarioSoporte.nombre, COUNT(t) FROM Ticket t WHERE t.usuarioSoporte IS NOT NULL GROUP BY t.usuarioSoporte.nombre")
    List<Object[]> contarPorIngeniero();

    /**
     * Calificacion media de cada tecnico, sobre los tickets ya encuestados.
     *
     * <p>Los tickets sin responder quedan fuera del promedio: contarlos como
     * cero castigaria a quien atiende a usuarios que no suelen contestar.</p>
     *
     * <p>Devuelve nombre, media ponderada y numero de respuestas. La cantidad
     * importa tanto como la media: un 3.0 sobre una sola encuesta no dice lo
     * mismo que un 2.8 sobre cuarenta.</p>
     */
    @Query("""
            SELECT t.usuarioSoporte.nombre,
                   AVG(CASE t.calificacion
                         WHEN com.sedif.sistema_tickets.util.enums.Calificacion.BUENO THEN 3.0
                         WHEN com.sedif.sistema_tickets.util.enums.Calificacion.REGULAR THEN 2.0
                         ELSE 1.0 END),
                   COUNT(t)
            FROM Ticket t
            WHERE t.usuarioSoporte IS NOT NULL AND t.calificacion IS NOT NULL
            GROUP BY t.usuarioSoporte.nombre
            """)
    List<Object[]> calificacionPromedioPorTecnico();

    /** Reparto global de calificaciones, para ver cuantos malos hubo. */
    @Query("""
            SELECT t.calificacion, COUNT(t)
            FROM Ticket t
            WHERE t.calificacion IS NOT NULL
            GROUP BY t.calificacion
            """)
    List<Object[]> contarPorCalificacion();

    /**
     * Altas por dia, en orden cronologico, para la grafica de tendencia.
     *
     * <p>El CAST a {@code date} descarta la hora: sin el, cada ticket formaria
     * su propio grupo por diferir en segundos.</p>
     */
    @Query("SELECT CAST(t.fechaCreacion AS date), COUNT(t) FROM Ticket t GROUP BY CAST(t.fechaCreacion AS date) ORDER BY CAST(t.fechaCreacion AS date) ASC")
    List<Object[]> contarPorFecha();

    @Query("SELECT t.prioridad, COUNT(t) FROM Ticket t GROUP BY t.prioridad")
    List<Object[]> contarPorPrioridad();
}
