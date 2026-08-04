package com.sedif.sistema_tickets.core.usuarios;

import com.sedif.sistema_tickets.util.enums.EstadoTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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

    /**
     * Listado paginado para el panel de administracion.
     *
     * <p>Antes la pantalla traia la tabla completa con {@code findAll()} y
     * filtraba en el navegador: la busqueda solo miraba lo ya descargado y
     * cada carga crecia con la plantilla.</p>
     *
     * <p>Los tres filtros son opcionales ({@code null} = sin filtrar). El
     * texto llega ya en minusculas y con comodines desde el servicio, y el
     * {@code CAST} fija su tipo: PostgreSQL no puede inferirlo cuando el
     * parametro es nulo dentro de {@code LOWER(...)} y la consulta falla con
     * "function lower(bytea) does not exist".</p>
     */
    @EntityGraph(attributePaths = {"rol", "area"})
    @Query("""
            SELECT u FROM Usuario u
            WHERE (CAST(:busqueda AS string) IS NULL
                   OR LOWER(u.nombre) LIKE :busqueda ESCAPE '!'
                   OR LOWER(u.apellidoPaterno) LIKE :busqueda ESCAPE '!'
                   OR LOWER(u.correo) LIKE :busqueda ESCAPE '!'
                   OR LOWER(u.username) LIKE :busqueda ESCAPE '!')
              AND (CAST(:rol AS string) IS NULL OR u.rol.nombre = :rol)
              AND (:activo IS NULL OR u.activo = :activo)
            """)
    Page<Usuario> buscarPaginado(@Param("busqueda") String busqueda,
                                 @Param("rol") String rol,
                                 @Param("activo") Boolean activo,
                                 Pageable pageable);

    boolean existsByCorreo(String correo);

    boolean existsByCorreoAndIdNot(String correo, Long id);

    /**
     * Comprueba si el nombre de usuario ya esta tomado.
     *
     * <p>La columna tiene restriccion UNIQUE, pero sin esta comprobacion previa
     * el choque lo detectaba la base de datos y el usuario recibia un 500
     * generico ("Ocurrio un error al procesar la solicitud") en lugar de
     * saber que el nombre estaba repetido.</p>
     */
    boolean existsByUsername(String username);

    // ==========================================
    // 3. MOTOR DE ASIGNACIÓN (TICKETS)
    // ==========================================
    
    @Query("SELECT u FROM Usuario u " +
           "LEFT JOIN Ticket t ON t.usuarioSoporte = u AND t.estado = :estado " +
           "WHERE u.rol.id = :rolId " +
           "AND u.activo = true " +
           "AND u.disponibleSoporte = true " +
           "AND u.id NOT IN (SELECT a.soporteFijo.id FROM Area a WHERE a.soporteFijo IS NOT NULL) " +
           "GROUP BY u " +
           "ORDER BY COUNT(t.id) ASC")
    List<Usuario> buscarTecnicosGlobalesOrdenadosPorCarga(@Param("rolId") Long rolId, @Param("estado") EstadoTicket estado);

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
     * @param estado estado que cuenta como carga viva (normalmente ABIERTO).
     */
    @Query("""
            SELECT u FROM Usuario u
            LEFT JOIN Ticket t ON t.usuarioSoporte = u AND t.estado = :estado
            WHERE u.rol.nombre = :nombreRol
              AND u.activo = true
              AND u.disponibleSoporte = true
            GROUP BY u
            ORDER BY COUNT(t.id) ASC
            """)
    List<Usuario> buscarTecnicosDisponiblesOrdenadosPorCarga(
            @Param("nombreRol") String nombreRol,
            @Param("estado") EstadoTicket estado);
}