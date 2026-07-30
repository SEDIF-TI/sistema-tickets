package com.sedif.sistema_tickets.core.resguardo;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.util.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "resguardo")
@Getter @Setter @NoArgsConstructor
public class Resguardo extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Long id;

    @Column(name = "s_solicitante_nombre", length = 100, nullable = false)
    private String solicitanteNombre;

    @Column(name = "s_solicitante_numero", length = 30, nullable = false)
    private String solicitanteNumero;

    @Column(name = "s_equipo_nombre", length = 100, nullable = false)
    private String equipoNombre;

    @Column(name = "s_numero_serie", length = 50, nullable = false)
    private String numeroSerie;

    @Column(name = "s_accesorios", columnDefinition = "TEXT")
    private String accesorios;

    @Column(name = "d_fecha_vencimiento")
    private LocalDateTime fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "s_estado_resguardo", length = 20, nullable = false)
    private EstadoResguardo estado;

    // === NUEVOS CAMPOS SINCRONIZADOS CON LA BASE DE DATOS ===
    @Column(name = "s_telefono", length = 50)
    private String telefono;

    @Column(name = "s_departamento", length = 100)
    private String departamento;

    @Column(name = "s_numero_inventario", length = 50)
    private String numeroInventario;

    @Column(name = "s_condiciones", columnDefinition = "TEXT")
    private String condiciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_usuario_creador_id", nullable = false)
    private Usuario usuarioCreador;
}