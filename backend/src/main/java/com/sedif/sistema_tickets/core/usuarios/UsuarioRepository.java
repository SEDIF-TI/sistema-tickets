package com.sedif.sistema_tickets.core.usuarios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    // Spring Data crea automáticamente la consulta SQL para verificar si un correo ya existe
    boolean existsByCorreo(String correo);
}
