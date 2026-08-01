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
    
    /** Usuarios de un rol concreto, por su nombre (ej. "SOPORTE"). */
    List<Usuario> findByRolNombre(String rolNombre);

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

    /**
     * Tecnicos activos y disponibles, ordenados de menor a mayor carga de
     * trabajo. La primera posicion es el candidato del balanceador.
     *
     * <p>Sustituye al calculo que hacia {@code TicketService}: alli se traia la
     * tabla de usuarios completa con {@code findAll()} y luego se lanzaba una
     * consulta COUNT por cada tecnico dentro del comparador (problema N+1 en
     * el camino critico de creacion de tickets). Aqui todo se resuelve en una
     * sola consulta agregada.</p>
     *
     * <p>A diferencia de {@code buscarTecnicosGlobalesOrdenadosPorCarga}, no
     * excluye a quienes son soporte fijo de un area: si el fijo de un area no
     * esta disponible, sus companeros deben poder recibir ese ticket aunque
     * ellos mismos sean fijos de otra area.</p>
     *
     * @param nombreRol     nombre del rol tecnico (normalmente "SOPORTE").
     * @param nombreEstatus estatus que cuenta como carga viva (ej. "ABIERTO").
     */
    @Query("""
            SELECT u FROM Usuario u
            LEFT JOIN Ticket t ON t.usuarioSoporte = u AND t.estatus.nombre = :nombreEstatus
            WHERE u.rol.nombre = :nombreRol
              AND u.activo = true
              AND u.disponibleSoporte = true
            GROUP BY u
            ORDER BY COUNT(t.id) ASC
            """)
    List<Usuario> buscarTecnicosDisponiblesOrdenadosPorCarga(
            @Param("nombreRol") String nombreRol,
            @Param("nombreEstatus") String nombreEstatus);
}