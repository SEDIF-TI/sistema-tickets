package com.sedif.sistema_tickets.core.area;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AreaRepository extends JpaRepository<Area, Long> {
    // Aquí después podemos agregar búsquedas personalizadas
}