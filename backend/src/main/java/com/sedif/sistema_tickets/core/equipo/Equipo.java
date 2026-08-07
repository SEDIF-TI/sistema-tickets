package com.sedif.sistema_tickets.core.equipo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Entrada del catalogo de equipos.
 *
 * <p>Reune las descripciones, marcas y modelos que ya se han tecleado alguna
 * vez, de modo que el formulario de dictamen pueda ofrecerlas por
 * autocompletado en lugar de obligar a escribirlas de nuevo.</p>
 */
@Entity
@Table(name = "equipo")
@Getter @Setter
@NoArgsConstructor
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifica la entrada del catalogo: no puede repetirse. */
    @Column(nullable = false, unique = true)
    private String descripcion; 

    private String marca;
    private String modelo;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    /**
     * Normaliza los datos antes de escribirlos.
     *
     * <p>Todo se guarda en mayusculas y sin espacios sobrantes para que la
     * restriccion UNIQUE de la descripcion sea efectiva: sin esto, "Laptop
     * Dell" y "LAPTOP DELL" convivirian como dos entradas distintas en un
     * catalogo cuyo proposito es justamente reutilizar lo ya registrado. La
     * fecha de registro se fija en el alta y no se vuelve a tocar.</p>
     */
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