package com.sedif.sistema_tickets.core.taller;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipoReparacionRepository extends JpaRepository<EquipoReparacion, Long> {

    Optional<EquipoReparacion> findByFolio(String folio);

    List<EquipoReparacion> findByEstadoTaller(String estadoTaller);

    // Búsqueda general por folio, solicitante, número de serie o número de inventario
    @Query("SELECT e FROM EquipoReparacion e WHERE " +
           "LOWER(e.folio) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(e.solicitanteNombre) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(e.numeroSerie) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(e.numeroInventario) LIKE LOWER(CONCAT('%', :filtro, '%'))")
    List<EquipoReparacion> buscarPorFiltro(@Param("filtro") String filtro);
}