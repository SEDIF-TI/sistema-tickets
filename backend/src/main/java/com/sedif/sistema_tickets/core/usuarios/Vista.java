package com.sedif.sistema_tickets.core.usuarios;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Pantalla del frontend que puede formar parte del menu.
 *
 * <p>El menu no esta escrito en el cliente: cada fila de esta tabla es una
 * entrada posible, y la tabla {@code rol_vista} decide que roles la ven. Anadir
 * o retirar una opcion del menu es cuestion de datos, no de recompilar el
 * frontend.</p>
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

    /** Texto que se muestra en la barra lateral, por ejemplo "Levantar Ticket". */
    @Column(name = "s_nombre", nullable = false, length = 100)
    private String nombre;

    /** Ruta del frontend a la que navega, por ejemplo "/admin/usuarios". */
    @Column(name = "s_ruta", nullable = false, length = 100)
    private String ruta;

    /** Nombre del icono que el frontend resuelve, por ejemplo "People". */
    @Column(name = "s_icono", length = 50)
    private String icono;

    @Column(name = "b_activo", nullable = false)
    private Boolean activo = true;
}