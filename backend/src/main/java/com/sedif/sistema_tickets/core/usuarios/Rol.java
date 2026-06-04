package com.sedif.sistema_tickets.core.usuarios;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.util.List;

@Entity
@Table(name = "rol")
@Getter @Setter @NoArgsConstructor
public class Rol {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Long id;

    @Column(name = "s_nombre", unique = true, nullable = false)
    private String nombre;

    @Column(name = "s_nivel_vision", nullable = false)
    private String nivelVision; // Valores: "GLOBAL", "AREA", "PERSONAL"

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "rol_vista", // El nombre de la tabla intermedia
        joinColumns = @JoinColumn(name = "fn_rol_id"), // La llave foránea hacia el rol
        inverseJoinColumns = @JoinColumn(name = "fn_vista_id") // La llave foránea hacia la vista
    )
    private List<Vista> vistas;
}