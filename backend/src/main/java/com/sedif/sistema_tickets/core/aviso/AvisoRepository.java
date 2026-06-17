package com.sedif.sistema_tickets.core.aviso;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface AvisoRepository extends JpaRepository<Aviso, Long> {
    // Traeremos solo los avisos activos para mostrarlos a las áreas
    List<Aviso> findByActivoTrueOrderByIdDesc();
}