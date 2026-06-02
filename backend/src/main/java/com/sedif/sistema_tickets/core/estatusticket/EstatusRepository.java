package com.sedif.sistema_tickets.core.estatusticket;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EstatusRepository extends JpaRepository<Estatus, Long> {
    Optional<Estatus> findByNombre(String nombre);
}