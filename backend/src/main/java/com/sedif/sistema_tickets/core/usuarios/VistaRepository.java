package com.sedif.sistema_tickets.core.usuarios;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VistaRepository extends JpaRepository<Vista, Long> {
    /** Vistas dadas de alta, para armar el menu dinamico. */
    List<Vista> findByActivoTrue();
}