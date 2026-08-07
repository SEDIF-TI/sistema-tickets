package com.sedif.sistema_tickets.core.dictamen;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DictamenRepository extends JpaRepository<Dictamen, Long> {

    /**
     * Listado paginado con busqueda opcional.
     *
     * <p>El texto llega ya en minusculas y con comodines desde el servicio. El
     * CAST fija el tipo del parametro: PostgreSQL no puede inferirlo cuando es
     * nulo dentro de LOWER(...), y falla con
     * {@code function lower(bytea) does not exist}. El ESCAPE '!' respeta el
     * escapado que el servicio aplica a '%', '_' y '!'.</p>
     *
     * <p>El EntityGraph trae al tecnico en la misma consulta, de modo que
     * convertir la pagina a DTO no dispare una consulta por fila.</p>
     */
    @EntityGraph(attributePaths = {"tecnico"})
    @Query("""
            SELECT d FROM Dictamen d
            WHERE (CAST(:busqueda AS string) IS NULL
                   OR LOWER(d.folio) LIKE :busqueda ESCAPE '!'
                   OR LOWER(d.nombreUsuario) LIKE :busqueda ESCAPE '!'
                   OR LOWER(d.descripcionEquipo) LIKE :busqueda ESCAPE '!'
                   OR LOWER(d.serie) LIKE :busqueda ESCAPE '!'
                   OR LOWER(d.marca) LIKE :busqueda ESCAPE '!'
                   OR LOWER(d.departamentoUsuario) LIKE :busqueda ESCAPE '!')
            """)
    Page<Dictamen> buscarPaginado(@Param("busqueda") String busqueda, Pageable pageable);

    /**
     * Mayor consecutivo de folio del ano indicado.
     *
     * <p>Mismo enfoque que en el taller: SUBSTRING extrae la parte numerica que
     * sigue al prefijo en folios con formato {@code DIC-2026-N} y se toma su
     * maximo, acotando la busqueda a los folios de ese ano. Asi la numeracion
     * arranca de nuevo en cada ejercicio y no depende del total de filas.</p>
     *
     * <p>Devuelve vacio cuando el ano no tiene folios todavia.</p>
     *
     * @param patron prefijo con comodin, por ejemplo {@code "DIC-2026-%"}.
     */
    @Query("""
            SELECT MAX(CAST(SUBSTRING(d.folio, LENGTH(:patron), LENGTH(d.folio)) AS integer))
            FROM Dictamen d
            WHERE d.folio LIKE :patron
            """)
    Optional<Integer> findMaxConsecutivoDelAnio(@Param("patron") String patron);
}
