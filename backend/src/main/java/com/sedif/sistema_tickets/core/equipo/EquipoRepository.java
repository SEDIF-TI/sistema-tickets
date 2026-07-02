package com.sedif.sistema_tickets.core.equipo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface EquipoRepository extends JpaRepository<Equipo, Long> {
    
    // Buscará coincidencias para el Autocomplete del frontend
    List<Equipo> findByDescripcionContainingIgnoreCase(String descripcion);
    
    // Verifica si ya existe para no duplicarlo al momento de autoguardar
    Optional<Equipo> findByDescripcionIgnoreCase(String descripcion);
}