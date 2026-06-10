package com.sedif.sistema_tickets.core.ticket.bitacora;

import com.sedif.sistema_tickets.core.ticket.Ticket;
import com.sedif.sistema_tickets.util.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

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