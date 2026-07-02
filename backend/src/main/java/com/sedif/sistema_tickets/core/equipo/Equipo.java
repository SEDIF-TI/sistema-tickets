package com.sedif.sistema_tickets.core.equipo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "equipo")
@Getter @Setter
@NoArgsConstructor
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String descripcion; 

    private String marca;
    private String modelo;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    @PrePersist
    @PreUpdate
    public void estandarizarDatos() {
        this.descripcion = (descripcion != null) ? descripcion.toUpperCase().trim() : null;
        this.marca = (marca != null) ? marca.toUpperCase().trim() : null;
        this.modelo = (modelo != null) ? modelo.toUpperCase().trim() : null;
        
        if (this.fechaRegistro == null) {
            this.fechaRegistro = LocalDateTime.now();
        }
    }
}