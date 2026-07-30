package com.sedif.sistema_tickets.core.taller;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.util.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "equipo_reparacion")
@Getter
@Setter
@NoArgsConstructor
public class EquipoReparacion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Long id;

    @Column(name = "s_folio", nullable = false, unique = true, length = 50)
    private String folio;

    @Column(name = "fn_ticket_id")
    private Long ticketId; // Opcional, por si viene de un ticket existente

    // Datos del Servidor Público dueño del equipo
    @Column(name = "s_solicitante_nombre", nullable = false, length = 200)
    private String solicitanteNombre;

    @Column(name = "s_solicitante_numero", length = 50)
    private String solicitanteNumero;

    @Column(name = "s_departamento", length = 150)
    private String departamento;

    // Características del dispositivo
    @Column(name = "s_equipo_tipo", nullable = false, length = 100)
    private String equipoTipo; // Laptop, PC Escritorio, Impresora, etc.

    @Column(name = "s_marca", length = 100)
    private String marca;

    @Column(name = "s_modelo", length = 100)
    private String modelo;

    @Column(name = "s_numero_serie", length = 100)
    private String numeroSerie;

    @Column(name = "s_numero_inventario", length = 100)
    private String numeroInventario;

    // Estado de recepción y taller
    @Column(name = "s_condicion_recepcion", length = 500)
    private String condicionRecepcion; // Rayado, estrellado, faltan teclas...

    @Column(name = "s_accesorios", length = 500)
    private String accesorios; // Cargador, mochila, mouse...

    @Column(name = "s_falla_reportada", length = 1000)
    private String fallaReportada;

    @Column(name = "s_diagnostico", length = 1000)
    private String diagnostico;

    @Column(name = "s_solucion", length = 1000)
    private String solucion;

    @Column(name = "s_estado_taller", nullable = false, length = 50)
    private String estadoTaller; // RECIBIDO, EN_DIAGNOSTICO, REPARADO, ENTREGADO

    // Técnico de soporte que atiende el equipo físicamente en el taller
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_tecnico_id")
    private Usuario tecnicoAsignado;
}