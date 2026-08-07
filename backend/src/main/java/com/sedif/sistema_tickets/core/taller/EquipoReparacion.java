package com.sedif.sistema_tickets.core.taller;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.util.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Equipo que entra al taller de TI para diagnostico y reparacion.
 *
 * <p>Reune la responsiva de recepcion (a quien pertenece el equipo, con que
 * accesorios y en que condiciones fisicas llega) y el avance tecnico (falla
 * reportada, diagnostico y solucion), de modo que el registro sirve tanto de
 * constancia de entrega como de historial de la reparacion.</p>
 *
 * <p>El folio es unico y lo genera el servidor con el formato
 * {@code REP-<ano>-<consecutivo>}; el estado sigue los valores de
 * {@code util.enums.EstadoTaller}.</p>
 */
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

    // Ticket de origen cuando la reparacion nace de un reporte. Es opcional:
    // el equipo tambien puede llegar al taller sin ticket previo.
    @Column(name = "fn_ticket_id")
    private Long ticketId;

    // Servidor publico al que pertenece el equipo
    @Column(name = "s_solicitante_nombre", nullable = false, length = 200)
    private String solicitanteNombre;

    @Column(name = "s_solicitante_numero", length = 50)
    private String solicitanteNumero;

    @Column(name = "s_departamento", length = 150)
    private String departamento;

    // Identificacion del dispositivo
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

    // Constancia del estado fisico en que se recibio el equipo: es lo que
    // permite deslindar danos previos de los ocurridos durante la reparacion.
    @Column(name = "s_condicion_recepcion", length = 500)
    private String condicionRecepcion; // Rayado, estrellado, faltan teclas...

    // Lo que entra junto con el equipo y debe devolverse con el.
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

    // Tecnico que atiende fisicamente el equipo en el taller. La relacion es
    // perezosa: el listado se resuelve con un EntityGraph explicito y no
    // arrastra al usuario en cada consulta que solo necesita el equipo.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_tecnico_id")
    private Usuario tecnicoAsignado;
}