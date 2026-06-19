package com.sedif.sistema_tickets.core.usuarios;

import com.sedif.sistema_tickets.core.estatusticket.Estatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    // ==========================================
    // 1. MÉTODOS DEL MÓDULO DE AUTENTICACIÓN
    // ==========================================
    
    // AGREGA ESTE MÉTODO: Es el que el TicketService busca para identificar al usuario por correo
    Optional<Usuario> findByCorreo(String correo);

    // Mantenemos este para el login dual (usuario o correo)
    Optional<Usuario> findByCorreoOrUsername(String correo, String username);

    // ==========================================
    // 2. MÉTODOS DE VALIDACIÓN DE PERFIL
    // ==========================================
    
    boolean existsByCorreo(String correo);

    boolean existsByCorreoAndIdNot(String correo, Long id);

    // ==========================================
    // 3. MOTOR DE ASIGNACIÓN (TICKETS)
    // ==========================================
    
    @Query("SELECT u FROM Usuario u " +
           "LEFT JOIN Ticket t ON t.usuarioSoporte = u AND t.estatus = :estatus " +
           "WHERE u.rol.id = :rolId " +
           "AND u.activo = true " +
           "AND u.disponibleSoporte = true " +
           "AND u.id NOT IN (SELECT a.soporteFijo.id FROM Area a WHERE a.soporteFijo IS NOT NULL) " +
           "GROUP BY u " +
           "ORDER BY COUNT(t.id) ASC")
    List<Usuario> buscarTecnicosGlobalesOrdenadosPorCarga(@Param("rolId") Long rolId, @Param("estatus") Estatus estatus);
}