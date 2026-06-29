package com.sedif.sistema_tickets.core.aviso;

import com.sedif.sistema_tickets.util.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "aviso")
@Getter
@Setter
@NoArgsConstructor
public class Aviso extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Long id;

    @Column(name = "s_titulo", nullable = false, length = 150)
    private String titulo;

    @Column(name = "s_mensaje", nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Column(name = "b_activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "pn_area_id", nullable = true)
    private Long areaId; 

    // ---> NUEVO: Campo para controlar la baja lógica
    @Column(name = "b_eliminado", nullable = false)
    private Boolean eliminado = false; 
}