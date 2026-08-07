package com.sedif.sistema_tickets.core.aviso;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface AvisoRepository extends JpaRepository<Aviso, Long> {

    /** Avisos vigentes para la barra de los paneles: activos y no dados de baja. */
    List<Aviso> findByActivoTrueAndEliminadoFalseOrderByIdDesc();

    /** Listado del panel de administracion: incluye los inactivos, no los de baja. */
    List<Aviso> findByEliminadoFalseOrderByIdDesc();

    /**
     * Avisos activos agrupados por area, para la grafica del panel.
     *
     * <p>La subconsulta resuelve el nombre del area a partir del
     * {@code areaId}, que no es una relacion JPA sino una columna suelta. Los
     * avisos sin area se agrupan bajo la etiqueta global.</p>
     */
    @Query("SELECT COALESCE((SELECT ar.nombre FROM Area ar WHERE ar.id = a.areaId), 'GLOBAL'), COUNT(a) FROM Aviso a WHERE a.activo = true AND a.eliminado = false GROUP BY a.areaId")
    List<Object[]> contarAvisosActivosPorArea();
}