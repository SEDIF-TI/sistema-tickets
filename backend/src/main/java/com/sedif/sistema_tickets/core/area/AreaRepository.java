package com.sedif.sistema_tickets.core.area;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AreaRepository extends JpaRepository<Area, Long> {

    /**
     * Listado paginado del panel de administracion.
     *
     * <p>Los filtros son opcionales ({@code null} = sin filtrar). El texto
     * llega ya en minusculas y con comodines desde el servicio, y el
     * {@code CAST} fija su tipo: PostgreSQL no puede inferirlo cuando el
     * parametro es nulo dentro de {@code LOWER(...)}.</p>
     */
    @EntityGraph(attributePaths = {"soporteFijo"})
    @Query("""
            SELECT a FROM Area a
            WHERE (CAST(:busqueda AS string) IS NULL
                   OR LOWER(a.nombre) LIKE :busqueda ESCAPE '!')
              AND (:activo IS NULL OR a.activo = :activo)
            """)
    Page<Area> buscarPaginado(@Param("busqueda") String busqueda,
                              @Param("activo") Boolean activo,
                              Pageable pageable);

    /** Comprueba si ya existe un area con ese nombre, sin distinguir mayusculas. */
    boolean existsByNombreIgnoreCase(String nombre);

    /** Igual que el anterior, excluyendo un area concreta: sirve al editar. */
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    /**
     * Cuenta los usuarios activos adscritos a un area.
     *
     * <p>Lo consulta la baja del area: desactivar una que todavia tiene
     * personal dejaria a esas personas sin area valida, y con ella se rompe el
     * filtro de visibilidad de sus tickets.</p>
     */
    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.area.id = :areaId AND u.activo = true")
    long contarUsuariosActivos(@Param("areaId") Long areaId);
}
