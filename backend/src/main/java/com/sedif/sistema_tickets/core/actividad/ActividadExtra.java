package com.sedif.sistema_tickets.core.actividad;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.util.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Actividad del plan anual de trabajo registrada a mano.
 *
 * <p>Recoge el trabajo que no llega por la via de un ticket. Junto con los
 * tickets cerrados, alimenta el reporte de actividades del periodo y las
 * metricas de avance frente a las metas fijadas a inicio de ano.</p>
 */
@Entity
@Table(name = "actividad_extra")
@Getter @Setter @NoArgsConstructor
public class ActividadExtra extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    @PrimaryKeyJoinColumn
    private Long id;

    /** Persona que realiza la tarea, tomada del token al registrarla. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_usuario_id", nullable = false)
    private Usuario usuario;

    /** Meta del plan de trabajo a la que se imputa, del 1 al 12. */
    @Column(name = "plan_trabajo_clave", nullable = false)
    private Integer planTrabajoClave;

    /** Encabezado de la actividad. Se imprime en la columna homonima del reporte. */
    @Column(name = "s_actividad_solicitada", nullable = false, length = 255)
    private String actividadSolicitada;

    /** Estado del avance, por ejemplo "COMPLETADO" o "EN PROCESO". */
    @Column(name = "s_situacion_actual", nullable = false, length = 50)
    private String situacionActual;

    /** Detalle de lo realizado. Ocupa la columna de actividad de solucion del reporte. */
    @Column(name = "s_justificacion", columnDefinition = "TEXT")
    private String justificacion;

    /** Momento en que se realizo. Es el campo por el que se acota el periodo del reporte. */
    @Column(name = "d_fecha_actividad", nullable = false)
    private LocalDateTime fechaActividad;
}