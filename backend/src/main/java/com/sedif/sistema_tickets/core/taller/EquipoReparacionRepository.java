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
     * es nulo dentro de LOWER(...).</p>
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
     * <p>Extrae la parte numerica que sigue al ultimo guion de folios con el
     * formato {@code REP-2026-N}. Sustituye al {@code count() + 1} anterior,
     * que producia folios duplicados cuando dos tecnicos registraban a la vez
     * y nunca reiniciaba la numeracion al cambiar de ano.</p>
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