package com.sedif.sistema_tickets.core.equipo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface EquipoRepository extends JpaRepository<Equipo, Long> {
    
    // Buscará coincidencias para el Autocomplete del frontend
    List<Equipo> findByDescripcionContainingIgnoreCase(String descripcion);
    
    // Verifica si ya existe para no duplicarlo al momento de autoguardar
    Optional<Equipo> findByDescripcionIgnoreCase(String descripcion);

    /** Igual que el anterior, excluyendo un equipo concreto: sirve al editar. */
    boolean existsByDescripcionIgnoreCaseAndIdNot(String descripcion, Long id);

    /**
     * Listado paginado con busqueda opcional sobre descripcion, marca y modelo.
     *
     * <p>El texto llega ya en minusculas y con comodines desde el servicio, y
     * el CAST fija su tipo: PostgreSQL no puede inferirlo cuando el parametro
     * es nulo dentro de LOWER(...).</p>
     */
    @Query("""
            SELECT e FROM Equipo e
            WHERE CAST(:busqueda AS string) IS NULL
               OR LOWER(e.descripcion) LIKE :busqueda ESCAPE '!'
               OR LOWER(e.marca) LIKE :busqueda ESCAPE '!'
               OR LOWER(e.modelo) LIKE :busqueda ESCAPE '!'
            """)
    Page<Equipo> buscarPaginado(@Param("busqueda") String busqueda, Pageable pageable);
}