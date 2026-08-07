package com.sedif.sistema_tickets.core.dictamen;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.util.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dictamen tecnico emitido por el area de TI.
 *
 * <p>Conserva todo lo que el formato oficial imprime, de modo que el historial
 * pueda consultarse y el documento reimprimirse sin volver a capturar nada.</p>
 *
 * <p>Las firmas se guardan como texto y no como referencia al usuario: el
 * documento debe conservar el nombre que llevaba impreso el dia que se emitio,
 * aunque despues esa persona cambie de puesto o cause baja. La relacion con el
 * tecnico existe aparte, solo para poder filtrar por responsable.</p>
 */
@Entity
@Table(name = "dictamen")
@Getter
@Setter
@NoArgsConstructor
public class Dictamen extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Long id;

    @Column(name = "s_folio", nullable = false, unique = true, length = 50)
    private String folio;

    /** Ticket del que nace el dictamen, si se emitio a partir de uno. */
    @Column(name = "fn_ticket_id")
    private Long ticketId;

    // ------------------------------------------------- equipo dictaminado
    @Column(name = "s_cve", length = 20)
    private String cve;

    @Column(name = "s_descripcion_equipo", length = 255)
    private String descripcionEquipo;

    @Column(name = "s_marca", length = 100)
    private String marca;

    @Column(name = "s_modelo", length = 100)
    private String modelo;

    @Column(name = "s_serie", length = 100)
    private String serie;

    @Column(name = "s_no_resguardo", length = 100)
    private String noResguardo;

    // ------------------------------------------------ servidor publico
    @Column(name = "s_nombre_usuario", length = 200)
    private String nombreUsuario;

    @Column(name = "s_telefono_usuario", length = 50)
    private String telefonoUsuario;

    @Column(name = "s_direccion_usuario", length = 200)
    private String direccionUsuario;

    @Column(name = "s_departamento_usuario", length = 150)
    private String departamentoUsuario;

    @Column(name = "s_tipo_reporte", length = 150)
    private String tipoReporte;

    // -------------------------------------------------- analisis tecnico
    // Los dos apartados que el formato oficial contempla: lo que el usuario
    // reporto y lo que el tecnico concluyo tras revisar el equipo.
    @Column(name = "s_falla_reportada", length = 1000)
    private String fallaReportada;

    @Column(name = "s_diagnostico", length = 1000)
    private String diagnostico;

    // ------------------------------------------------------------ firmas
    // Nombres tal como se imprimieron: son constancia del documento emitido y
    // no deben seguir los cambios posteriores del directorio de usuarios.
    @Column(name = "s_realizado_por", length = 200)
    private String realizadoPor;

    @Column(name = "s_revisado_por", length = 200)
    private String revisadoPor;

    @Column(name = "s_recibido_por", length = 200)
    private String recibidoPor;

    /** Tecnico que emitio el dictamen, para filtrar por responsable. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_tecnico_id")
    private Usuario tecnico;
}
