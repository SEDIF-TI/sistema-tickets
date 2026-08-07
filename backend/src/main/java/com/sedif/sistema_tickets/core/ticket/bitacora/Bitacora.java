package com.sedif.sistema_tickets.core.ticket.bitacora;

import com.sedif.sistema_tickets.core.ticket.Ticket;
import com.sedif.sistema_tickets.util.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Movimiento registrado en la historia de un ticket.
 *
 * <p>Cada transicion de estado deja una fila con el estado al que se paso y el
 * texto que lo justifica. Son registros de solo insercion: el ticket guarda su
 * situacion actual, y la bitacora, como se llego hasta ella.</p>
 *
 * <p>El estado se guarda como texto y no como enum porque es una constancia
 * historica: debe seguir leyendose tal cual quedo aunque el catalogo de estados
 * cambie.</p>
 */
@Entity
@Table(name = "bitacora")
@Getter @Setter @NoArgsConstructor
public class Bitacora extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_ticket_id", nullable = false)
    private Ticket ticket;

    @Column(name = "s_estatus_registrado", nullable = false, length = 50)
    private String estatusRegistrado;

    @Column(name = "s_justificacion", columnDefinition = "TEXT")
    private String justificacion;
}