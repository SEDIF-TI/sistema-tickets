package com.sedif.sistema_tickets.core.area;

import org.springframework.data.jpa.repository.JpaRepository;


public interface AreaRepository extends JpaRepository<Area, Long> {
    // Aquí después podemos agregar búsquedas personalizadas
}