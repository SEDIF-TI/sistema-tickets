package com.sedif.sistema_tickets.core.estatusticket;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "estadoticket")
@Getter @Setter @NoArgsConstructor
public class Estatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Long id;

    @Column(name = "s_nombre", nullable = false, unique = true, length = 50)
    private String nombre;

    public Estatus(String nombre) {
        this.nombre = nombre;
    }
}