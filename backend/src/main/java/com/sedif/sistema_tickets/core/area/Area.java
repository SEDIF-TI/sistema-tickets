package com.sedif.sistema_tickets.core.area;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.util.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;



/**
 * Area administrativa a la que se adscribe el personal.
 *
 * <p>Determina la visibilidad de los tickets de quienes pertenecen a ella y el
 * reparto del trabajo de soporte: si el area tiene un tecnico fijo, sus
 * tickets van directos a el en lugar de pasar por el balanceador de carga.</p>
 */
@Entity
@Table(name = "area")
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

    /**
     * Tecnico asignado en exclusiva al area. Nulo deja sus tickets en manos del
     * balanceador automatico.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_soporte_fijo_id")
    private Usuario soporteFijo;

    /** Un area prioritaria recibe atencion preferente sobre el resto. */
    @Column(name = "b_prioritaria", nullable = false)
    private Boolean prioritaria = false;

}