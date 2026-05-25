package com.sedif.sistema_tickets.core.usuarios;

import com.sedif.sistema_tickets.core.area.Area;
import com.sedif.sistema_tickets.util.audit.Auditable;
import com.sedif.sistema_tickets.util.enums.RolUsuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa a los usuarios del sistema.
 * Utiliza Lombok para la generación automática de métodos de acceso y constructores.
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class Usuario extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Long id;

    @Column(name = "s_nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "s_correo", nullable = false, unique = true, length = 100)
    private String correo;

    @Column(name = "s_password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "s_rol", nullable = false)
    private RolUsuario rol;

    @Column(name = "b_activo", nullable = false)
    private Boolean activo = true;

    // Control de disponibilidad para usuarios con rol SOPORTE gestionado por el Administrador.
    @Column(name = "b_disponible_soporte", nullable = false)
    private Boolean disponibleSoporte = false;

    // Relación muchos a uno: Múltiples usuarios de rol AREA pueden pertenecer a una misma Área.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_area_id")
    private Area area;
}