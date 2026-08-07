package com.sedif.sistema_tickets.core.ticket;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import java.time.LocalDateTime;
import com.sedif.sistema_tickets.util.audit.Auditable;
import com.sedif.sistema_tickets.util.enums.Calificacion;
import com.sedif.sistema_tickets.util.enums.EstadoTicket;
import com.sedif.sistema_tickets.util.enums.Prioridad;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ticket")
@Getter @Setter @NoArgsConstructor
public class Ticket extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Long id;

    @Column(name = "s_titulo", nullable = false)
    private String titulo;

    @Column(name = "s_sede")
    private String sede;

    @Column(name = "s_descripcion", nullable = false, columnDefinition = "TEXT")
    private String descripcion;
    
    // Persona que fisicamente sufre la falla, que no siempre es quien levanta
    // el ticket: soporte puede reportar en nombre de un tercero.
    @Column(name = "s_solicitante_nombre", length = 100)
    private String solicitanteNombre;

    @Column(name = "s_justificacion", columnDefinition = "TEXT")
    private String justificacion;

    /**
     * Estado del ticket dentro de su ciclo de vida: ABIERTO, EN_PROCESO o
     * CERRADO.
     *
     * <p>Vive en el enum {@link EstadoTicket} y no en una tabla catalogo: son
     * valores fijos, y resolverlos en memoria evita un JOIN en cada consulta y
     * la dependencia de que esas filas existan en la base.</p>
     *
     * <p>Se guarda como texto ({@code EnumType.STRING}) y no por su posicion:
     * con {@code ORDINAL}, insertar un valor nuevo en medio del enum
     * reinterpretaria en silencio todos los registros historicos.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "s_estado", nullable = false, length = 20)
    private EstadoTicket estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_usuario_area_id", nullable = false)
    private Usuario usuarioArea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_usuario_soporte_id")
    private Usuario usuarioSoporte;

    /**
     * Prioridad de atencion, acotada al enum {@link Prioridad}.
     *
     * <p>El enum cierra el conjunto de valores admitidos: si la columna
     * aceptara texto libre, un error de escritura crearia una categoria nueva
     * en las graficas del panel que agrupan por prioridad.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "s_prioridad", nullable = false, length = 20)
    private Prioridad prioridad;

    @Column(name = "d_fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "plan_trabajo_clave")
    private Integer planTrabajoClave;

    /**
     * Encuesta de satisfaccion que responde el solicitante al cerrar el ticket.
     *
     * <p>Nula mientras no se conteste: la encuesta es voluntaria, y "sin
     * responder" no es lo mismo que una mala calificacion.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "s_calificacion", length = 20)
    private Calificacion calificacion;

    @Column(name = "s_comentario_encuesta", length = 500)
    private String comentarioEncuesta;

    @Column(name = "d_fecha_encuesta")
    private LocalDateTime fechaEncuesta;
}