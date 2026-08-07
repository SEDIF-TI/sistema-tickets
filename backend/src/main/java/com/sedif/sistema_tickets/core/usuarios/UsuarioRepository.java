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
    
    /** Localiza al usuario por su correo, que es unico. */
    Optional<Usuario> findByCorreo(String correo);

    /**
     * Busqueda para el acceso dual: el principal del token puede ser el correo
     * o el nombre de usuario, asi que ambos parametros reciben el mismo valor.
     */
    Optional<Usuario> findByCorreoOrUsername(String correo, String username);

    /** Usuarios de un rol concreto, por su nombre (ej. "SOPORTE"). */
    List<Usuario> findByRolNombre(String rolNombre);

    /**
     * Listado paginado para el panel de administracion, con la busqueda y los
     * filtros resueltos en la base de datos.
     *
     * <p>Los tres filtros son opcionales ({@code null} = sin filtrar). El
     * texto llega ya en minusculas y con comodines desde el servicio, y el
     * {@code CAST} fija su tipo: PostgreSQL no puede inferirlo cuando el
     * parametro es nulo dentro de {@code LOWER(...)}, y la consulta falla con
     * "function lower(bytea) does not exist".</p>
     *
     * <p>El {@code @EntityGraph} trae rol y area en la misma consulta, ya que
     * el mapeo a DTO lee el nombre de ambos para cada fila de la pagina.</p>
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
     * <p>La columna tiene restriccion UNIQUE; adelantar aqui la comprobacion
     * permite responder con un mensaje que explica el choque, en lugar del 500
     * generico que produce el error de la base de datos.</p>
     */
    boolean existsByUsername(String username);

    /**
     * Tecnicos globales ordenados de menor a mayor carga viva.
     *
     * <p>Excluye a quienes ya son soporte fijo de algun area, porque su tiempo
     * esta comprometido con ella y no deben entrar en el reparto general.</p>
     */
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
     * <p>El LEFT JOIN cuenta solo los tickets en el estado indicado, de modo
     * que la carga refleje el trabajo vivo y no el historico. Al resolverse
     * como una unica consulta agregada, la asignacion no lanza un COUNT por
     * tecnico en el camino critico de creacion del ticket.</p>
     *
     * <p>A diferencia de {@link #buscarTecnicosGlobalesOrdenadosPorCarga}, no
     * excluye a quienes son soporte fijo de un area: si el fijo de un area no
     * esta disponible, sus companeros deben poder recibir ese ticket aunque
     * ellos mismos sean fijos de otra area.</p>
     *
     * @param nombreRol nombre del rol tecnico (normalmente "SOPORTE").
     * @param estado    estado que cuenta como carga viva (normalmente ABIERTO).
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