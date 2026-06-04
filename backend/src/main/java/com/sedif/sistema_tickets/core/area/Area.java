package com.sedif.sistema_tickets.core.area;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.util.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;



@Entity
@Table(name = "area") // Tabla en singular, correctamente aplicada.
@Getter
@Setter
@NoArgsConstructor
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
    
    @Column(name = "b_prioritaria", nullable = false)
    private Boolean prioritaria = false; // Por defecto, las áreas nuevas no tienen prioridad especial

        // Se eliminaron los constructores y getters/setters manuales gracias a Lombok.

}