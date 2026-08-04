package com.sedif.sistema_tickets.core.correo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CorreoInstitucionalRepository extends JpaRepository<CorreoInstitucional, Long> {

    /**
     * Directorio paginado con busqueda y filtro de estado, ambos opcionales.
     *
     * <p>El texto llega ya en minusculas y con comodines desde el servicio, y
     * el CAST fija su tipo: PostgreSQL no puede inferirlo cuando el parametro
     * es nulo dentro de LOWER(...).</p>
     */
    @org.springframework.data.jpa.repository.Query("""
            SELECT c FROM CorreoInstitucional c
            WHERE (CAST(:busqueda AS string) IS NULL
                   OR LOWER(c.nombre) LIKE :busqueda ESCAPE '!'
                   OR LOWER(c.apellidoPaterno) LIKE :busqueda ESCAPE '!'
                   OR LOWER(c.correo) LIKE :busqueda ESCAPE '!'
                   OR LOWER(c.area) LIKE :busqueda ESCAPE '!'
                   OR LOWER(c.cargo) LIKE :busqueda ESCAPE '!')
              AND (CAST(:estado AS string) IS NULL OR c.estado = :estado)
            """)
    org.springframework.data.domain.Page<CorreoInstitucional> buscarPaginado(
            @org.springframework.data.repository.query.Param("busqueda") String busqueda,
            @org.springframework.data.repository.query.Param("estado") String estado,
            org.springframework.data.domain.Pageable pageable);


    Optional<CorreoInstitucional> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    boolean existsByCorreoAndIdNot(String correo, Long id);

    List<CorreoInstitucional> findByEstado(String estado);

    // Búsqueda por texto (incluyendo todos los estados)
    @Query("SELECT c FROM CorreoInstitucional c WHERE " +
           "LOWER(c.correo) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(c.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(c.apellidoPaterno) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(c.area) LIKE LOWER(CONCAT('%', :filtro, '%'))")
    List<CorreoInstitucional> buscarPorFiltro(@Param("filtro") String filtro);

    // Búsqueda combinada por texto y estado
    @Query("SELECT c FROM CorreoInstitucional c WHERE " +
           "(LOWER(c.correo) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(c.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(c.apellidoPaterno) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(c.area) LIKE LOWER(CONCAT('%', :filtro, '%'))) AND " +
           "UPPER(c.estado) = UPPER(:estado)")
    List<CorreoInstitucional> buscarPorFiltroYEstado(@Param("filtro") String filtro, @Param("estado") String estado);
}