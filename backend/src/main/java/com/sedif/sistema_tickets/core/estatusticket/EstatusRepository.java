package com.sedif.sistema_tickets.core.estatusticket;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EstatusRepository extends JpaRepository<Estatus, Long> {
    Optional<Estatus> findByNombre(String nombre);
}