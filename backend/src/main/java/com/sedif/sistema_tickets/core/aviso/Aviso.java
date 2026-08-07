package com.sedif.sistema_tickets.core.aviso;

import com.sedif.sistema_tickets.util.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Aviso que se muestra en la barra de los paneles.
 *
 * <p>Maneja dos banderas independientes. {@code activo} controla si el aviso
 * se esta mostrando y el administrador lo alterna a voluntad;
 * {@code eliminado} es la baja logica, y retira el aviso de todos los listados
 * sin borrar la fila.</p>
 */
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

    /** Area destinataria. Nulo significa que el aviso es global. */
    @Column(name = "pn_area_id", nullable = true)
    private Long areaId; 

    /** Baja logica: el aviso desaparece de los listados y conserva su historial. */
    @Column(name = "b_eliminado", nullable = false)
    private Boolean eliminado = false; 
}