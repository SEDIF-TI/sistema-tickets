package com.sedif.sistema_tickets.core.ticket;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import java.time.LocalDateTime;
import com.sedif.sistema_tickets.util.audit.Auditable;
import com.sedif.sistema_tickets.util.enums.EstadoTicket;
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
    
    // ---> NUEVO CAMPO: Guardará el nombre de quien físicamente tiene la falla
    @Column(name = "s_solicitante_nombre", length = 100)
    private String solicitanteNombre;

    @Column(name = "s_justificacion", columnDefinition = "TEXT")
    private String justificacion;

    /**
     * Estado del ticket.
     *
     * <p>Antes era un {@code @ManyToOne} contra la tabla catalogo
     * {@code estadoticket}, retirada en la migracion V4: eran tres filas fijas
     * que obligaban a un JOIN en cada consulta y dejaban el sistema inservible
     * si la tabla llegaba vacia a una base nueva.</p>
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

    @Column(name = "s_prioridad", nullable = false, length = 20)
    private String prioridad;

    @Column(name = "d_fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "plan_trabajo_clave")
    private Integer planTrabajoClave;
}