package com.sedif.sistema_tickets.core.aviso;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AvisoRepository extends JpaRepository<Aviso, Long> {
    
    // Para las pantallas de los usuarios (Solo activos)
    List<Aviso> findByActivoTrueOrderByIdDesc();
    
    // Para el panel del Administrador (Todos los avisos, activos e inactivos)
    List<Aviso> findAllByOrderByIdDesc();
}