package com.sedif.sistema_tickets.core.area;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.util.audit.Auditable;
import jakarta.persistence.*;



@Entity
@Table(name = "areas")
public class Area extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Long id;

    @Column(name = "s_nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "b_activo", nullable = false)
    private Boolean activo = true;

    // Relación uno a uno: Un área puede tener asignado un único usuario de soporte fijo.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_soporte_fijo_id")
    private Usuario soporteFijo;

    // --- Constructor ---
    public Area() {}

    // --- Getters y Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public Usuario getSoporteFijo() { return soporteFijo; }
    public void setSoporteFijo(Usuario soporteFijo) { this.soporteFijo = soporteFijo; }
}