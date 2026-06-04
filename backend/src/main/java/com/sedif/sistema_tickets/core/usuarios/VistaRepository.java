package com.sedif.sistema_tickets.core.usuarios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VistaRepository extends JpaRepository<Vista, Long> {
    // Si más adelante queremos buscar vistas activas por nombre, Spring lo hace solo
    List<Vista> findByActivoTrue();
}