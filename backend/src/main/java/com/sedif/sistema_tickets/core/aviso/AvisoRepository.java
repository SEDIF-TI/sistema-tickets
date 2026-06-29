package com.sedif.sistema_tickets.core.aviso;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface AvisoRepository extends JpaRepository<Aviso, Long> {
    
    // Filtramos para que solo traiga los activos Y que no estén eliminados
    List<Aviso> findByActivoTrueAndEliminadoFalseOrderByIdDesc();
    
    // Para el panel del Administrador (Trae todos los no eliminados)
    List<Aviso> findByEliminadoFalseOrderByIdDesc();

    // ---> NUEVO: Consulta para la gráfica del Dashboard (Cuenta avisos activos agrupados por área)
    @Query("SELECT COALESCE((SELECT ar.nombre FROM Area ar WHERE ar.id = a.areaId), 'GLOBAL'), COUNT(a) FROM Aviso a WHERE a.activo = true AND a.eliminado = false GROUP BY a.areaId")
    List<Object[]> contarAvisosActivosPorArea();
}