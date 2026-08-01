package com.sedif.sistema_tickets.core.actividad;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActividadExtraRepository extends JpaRepository<ActividadExtra, Long> {

    /** Reporte por periodo, para el administrador. */
    List<ActividadExtra> findByFechaActividadBetween(LocalDateTime inicio, LocalDateTime fin);

    /** Reporte por periodo de una persona concreta. */
    List<ActividadExtra> findByUsuario_IdAndFechaActividadBetween(
            Long usuarioId, LocalDateTime inicio, LocalDateTime fin);

    /** Actividades de una persona, paginadas. */
    Page<ActividadExtra> findByUsuarioId(Long usuarioId, Pageable pageable);

    /**
     * Avance por meta del plan de trabajo: cuantas actividades se han
     * registrado para cada clave. Alimenta las metricas del panel.
     */
    @Query("SELECT a.planTrabajoClave, COUNT(a) FROM ActividadExtra a GROUP BY a.planTrabajoClave")
    List<Object[]> contarPorPlanTrabajo();

    /** Actividades registradas por cada persona del area. */
    @Query("SELECT a.usuario.nombre, COUNT(a) FROM ActividadExtra a GROUP BY a.usuario.nombre")
    List<Object[]> contarPorUsuario();
}
