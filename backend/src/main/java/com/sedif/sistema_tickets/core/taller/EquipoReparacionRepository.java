package com.sedif.sistema_tickets.core.taller;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipoReparacionRepository extends JpaRepository<EquipoReparacion, Long> {

    /**
     * Listado paginado con busqueda y filtro de estado, ambos opcionales.
     *
     * <p>El texto llega ya en minusculas y con comodines desde el servicio, y
     * el CAST fija su tipo: PostgreSQL no puede inferirlo cuando el parametro
     * es nulo dentro de LOWER(...) y la consulta falla con
     * {@code function lower(bytea) does not exist}. El ESCAPE '!' respeta el
     * escapado que el servicio aplica a '%', '_' y '!'.</p>
     *
     * <p>El EntityGraph trae al tecnico asignado en la misma consulta, de modo
     * que convertir la pagina a DTO no dispare una consulta por fila.</p>
     */
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"tecnicoAsignado"})
    @org.springframework.data.jpa.repository.Query("""
            SELECT e FROM EquipoReparacion e
            WHERE (CAST(:busqueda AS string) IS NULL
                   OR LOWER(e.folio) LIKE :busqueda ESCAPE '!'
                   OR LOWER(e.solicitanteNombre) LIKE :busqueda ESCAPE '!'
                   OR LOWER(e.equipoTipo) LIKE :busqueda ESCAPE '!'
                   OR LOWER(e.marca) LIKE :busqueda ESCAPE '!'
                   OR LOWER(e.numeroSerie) LIKE :busqueda ESCAPE '!'
                   OR LOWER(e.departamento) LIKE :busqueda ESCAPE '!')
              AND (CAST(:estado AS string) IS NULL OR e.estadoTaller = :estado)
            """)
    org.springframework.data.domain.Page<EquipoReparacion> buscarPaginado(
            @org.springframework.data.repository.query.Param("busqueda") String busqueda,
            @org.springframework.data.repository.query.Param("estado") String estado,
            org.springframework.data.domain.Pageable pageable);

    Optional<EquipoReparacion> findByFolio(String folio);

    List<EquipoReparacion> findByEstadoTaller(String estadoTaller);

    /**
     * Busqueda general por folio, solicitante, numero de serie o inventario.
     *
     * <p>El termino viaja como parametro vinculado ({@code :filtro}), nunca
     * concatenado en la consulta: el motor lo trata como valor y no como SQL,
     * asi que no hay superficie para inyeccion.</p>
     */
    @Query("SELECT e FROM EquipoReparacion e WHERE " +
           "LOWER(e.folio) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(e.solicitanteNombre) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(e.numeroSerie) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(e.numeroInventario) LIKE LOWER(CONCAT('%', :filtro, '%'))")
    List<EquipoReparacion> buscarPorFiltro(@Param("filtro") String filtro);

    /**
     * Mayor consecutivo de folio del ano indicado.
     *
     * <p>Extrae con SUBSTRING la parte numerica que sigue al prefijo en folios
     * con formato {@code REP-2026-N} y toma su maximo, acotando la busqueda a
     * los folios de ese ano. Asi la numeracion arranca de nuevo en cada
     * ejercicio y el consecutivo no depende del total de filas.</p>
     *
     * <p>Devuelve vacio cuando el ano no tiene folios todavia.</p>
     *
     * @param patron prefijo con comodin, por ejemplo {@code "REP-2026-%"}.
     */
    @Query("""
            SELECT MAX(CAST(SUBSTRING(e.folio, LENGTH(:patron), LENGTH(e.folio)) AS integer))
            FROM EquipoReparacion e
            WHERE e.folio LIKE :patron
            """)
    Optional<Integer> findMaxConsecutivoDelAnio(@Param("patron") String patron);

    /** Conteo por estado, para las metricas del panel. */
    long countByEstadoTaller(String estadoTaller);
}