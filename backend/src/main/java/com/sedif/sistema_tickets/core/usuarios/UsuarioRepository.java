package com.sedif.sistema_tickets.core.usuarios;

import com.sedif.sistema_tickets.util.enums.RolUsuario;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    // Spring Data crea automáticamente la consulta SQL para verificar si un correo ya existe
    boolean existsByCorreo(String correo);

    // Verifica si existe un correo específico, excluyendo el ID del usuario actual
    boolean existsByCorreoAndIdNot(String correo, Long id);

    /**
     * Algoritmo Round-Robin optimizado en base de datos.
     * Trae a los técnicos disponibles, que NO están casados con ningún área,
     * y los ordena de menor a mayor cantidad de tickets abiertos.
     */
    @Query("SELECT u FROM Usuario u " +
           "LEFT JOIN Ticket t ON t.usuarioSoporte = u AND t.estatus = :estatus " +
           "WHERE u.rol = :rol " +
           "AND u.activo = true " +
           "AND u.disponibleSoporte = true " +
           "AND u.id NOT IN (SELECT a.soporteFijo.id FROM Area a WHERE a.soporteFijo IS NOT NULL) " +
           "GROUP BY u " +
           "ORDER BY COUNT(t.id) ASC")

    List<Usuario> buscarTecnicosGlobalesOrdenadosPorCarga(
                            @Param("rol") RolUsuario rol, 
                            @Param("estatus") String estatus, 
                            Pageable pageable);
}
