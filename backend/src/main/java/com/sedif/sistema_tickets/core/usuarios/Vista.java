package com.sedif.sistema_tickets.core.usuarios;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa las pantallas o módulos del frontend 
 * configurables dinámicamente desde la base de datos.
 */
@Entity
@Table(name = "vista")
@Getter
@Setter
@NoArgsConstructor
public class Vista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Long id;

    @Column(name = "s_nombre", nullable = false, length = 100)
    private String nombre; // Ejemplo: "Levantar Ticket" o "Usuarios"

    @Column(name = "s_ruta", nullable = false, length = 100)
    private String ruta; // Ejemplo: "/empleado/nuevo" o "/admin/usuarios"

    @Column(name = "s_icono", length = 50)
    private String icono; // Ejemplo: "AddCircle" o "People"

    @Column(name = "b_activo", nullable = false)
    private Boolean activo = true;
}