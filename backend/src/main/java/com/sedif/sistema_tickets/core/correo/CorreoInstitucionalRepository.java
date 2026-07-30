package com.sedif.sistema_tickets.core.correo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CorreoInstitucionalRepository extends JpaRepository<CorreoInstitucional, Long> {

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