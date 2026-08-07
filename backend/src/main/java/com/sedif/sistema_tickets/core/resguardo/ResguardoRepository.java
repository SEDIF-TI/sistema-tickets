package com.sedif.sistema_tickets.core.resguardo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ResguardoRepository extends JpaRepository<Resguardo, Long> {

    /**
     * Listado paginado con busqueda y filtro de estado, ambos opcionales.
     *
     * <p>El texto llega ya en minusculas y con comodines desde el servicio, y
     * el CAST fija su tipo: PostgreSQL no puede inferirlo cuando el parametro
     * es nulo dentro de LOWER(...) y la consulta falla con
     * {@code function lower(bytea) does not exist}. El ESCAPE '!' respeta el
     * escapado que el servicio aplica a '%', '_' y '!'.</p>
     *
     * <p>El estado no necesita CAST porque llega tipado como enum. El
     * EntityGraph trae al usuario creador en la misma consulta, de modo que
     * convertir la pagina a DTO no dispare una consulta por fila.</p>
     */
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"usuarioCreador"})
    @org.springframework.data.jpa.repository.Query("""
            SELECT r FROM Resguardo r
            WHERE (CAST(:busqueda AS string) IS NULL
                   OR LOWER(r.solicitanteNombre) LIKE :busqueda ESCAPE '!'
                   OR LOWER(r.equipoNombre) LIKE :busqueda ESCAPE '!'
                   OR LOWER(r.numeroSerie) LIKE :busqueda ESCAPE '!'
                   OR LOWER(r.numeroInventario) LIKE :busqueda ESCAPE '!'
                   OR LOWER(r.departamento) LIKE :busqueda ESCAPE '!')
              AND (:estado IS NULL OR r.estado = :estado)
            """)
    org.springframework.data.domain.Page<Resguardo> buscarPaginado(
            @org.springframework.data.repository.query.Param("busqueda") String busqueda,
            @org.springframework.data.repository.query.Param("estado") EstadoResguardo estado,
            org.springframework.data.domain.Pageable pageable);

    List<Resguardo> findAllByOrderByFechaCreacionDesc();

    /**
     * Resguardos cuya fecha de vencimiento ya paso y siguen sin devolverse.
     * Es la consulta que alimenta la revision programada de vencimientos.
     */
    List<Resguardo> findByFechaVencimientoBeforeAndEstado(LocalDateTime fecha, EstadoResguardo estado);

    /**
     * Resguardos que venceran dentro de una ventana de tiempo.
     *
     * <p>Acotada a los que siguen ENTREGADO, permite avisar al personal antes
     * de que el prestamo caduque en lugar de reclamarlo cuando ya vencio.</p>
     */
    List<Resguardo> findByEstadoAndFechaVencimientoBetween(
            EstadoResguardo estado, LocalDateTime desde, LocalDateTime hasta);

    /** Conteo por estado, para las metricas del panel. */
    long countByEstado(EstadoResguardo estado);
}
