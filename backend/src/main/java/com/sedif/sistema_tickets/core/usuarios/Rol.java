package com.sedif.sistema_tickets.core.usuarios;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

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
}