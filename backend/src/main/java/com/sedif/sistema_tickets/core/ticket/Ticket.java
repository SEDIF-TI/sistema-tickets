package com.sedif.sistema_tickets.core.ticket;

import com.sedif.sistema_tickets.core.usuarios.Usuario;

import java.time.LocalDateTime;

import com.sedif.sistema_tickets.core.estatusticket.Estatus; 
import com.sedif.sistema_tickets.util.audit.Auditable;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_estadoticket_id", nullable = false)
    private Estatus estatus; 

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
}