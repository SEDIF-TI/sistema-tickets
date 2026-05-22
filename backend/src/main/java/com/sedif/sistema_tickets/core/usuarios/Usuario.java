package com.sedif.sistema_tickets.core.usuarios; // <-- Cambiado a sedif y a usuarios

import com.sedif.sistema_tickets.core.area.Area;
import com.sedif.sistema_tickets.util.audit.Auditable;
import com.sedif.sistema_tickets.util.enums.RolUsuario;
import jakarta.persistence.*;

// ... el resto de tu código hacia abajo queda igual ...

@Entity
@Table(name = "usuarios")
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

    // --- Constructor ---
    public Usuario() {}

    // --- Getters y Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public RolUsuario getRol() { return rol; }
    public void setRol(RolUsuario rol) { this.rol = rol; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public Boolean getDisponibleSoporte() { return disponibleSoporte; }
    public void setDisponibleSoporte(Boolean disponibleSoporte) { this.disponibleSoporte = disponibleSoporte; }

    public Area getArea() { return area; }
    public void setArea(Area area) { this.area = area; }
}