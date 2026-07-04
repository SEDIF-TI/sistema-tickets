package com.sedif.sistema_tickets.core.actividad;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.util.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "actividad_extra")
@Getter @Setter @NoArgsConstructor
public class ActividadExtra extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    @PrimaryKeyJoinColumn
    private Long id;

    // Relación con el usuario (Desarrollador, Administrativo o Soporte) que realiza la tarea
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_usuario_id", nullable = false)
    private Usuario usuario;

    // Clave del Plan de Trabajo (del 1 al 12)
    @Column(name = "plan_trabajo_clave", nullable = false)
    private Integer planTrabajoClave;

    // Equivalente a "Actividad Solicitada" (ej: "Desarrollo de módulo de gráficas")
    @Column(name = "s_actividad_solicitada", nullable = false, length = 255)
    private String actividadSolicitada;

    // Equivalente a "Situación Actual" (ej: "COMPLETADO", "EN PROCESO")
    @Column(name = "s_situacion_actual", nullable = false, length = 50)
    private String situacionActual;

    // Equivalente a "Actividad de Solución" / Justificación detallada
    @Column(name = "s_justificacion", columnDefinition = "TEXT")
    private String justificacion;

    // Fecha en la que se realizó la actividad para los filtros diarios/semanales
    @Column(name = "d_fecha_actividad", nullable = false)
    private LocalDateTime fechaActividad;
}