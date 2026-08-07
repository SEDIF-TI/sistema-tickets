package com.sedif.sistema_tickets.core.usuarios;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Rol del sistema y las vistas que habilita.
 *
 * <p>Es el catalogo fijo sobre el que giran los permisos: ADMINISTRADOR,
 * SOPORTE y EMPLEADO. Cada rol determina dos cosas distintas. El
 * {@code nivelVision} decide cuantos tickets alcanza a ver quien lo ostenta, y
 * la coleccion {@code vistas} define el menu que se le dibuja.</p>
 */
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

    /** Alcance de la bandeja de tickets: "GLOBAL", "AREA" o "PERSONAL". */
    @Column(name = "s_nivel_vision", nullable = false)
    private String nivelVision;

    /**
     * Vistas del menu que este rol habilita, a traves de la tabla intermedia
     * {@code rol_vista}.
     *
     * <p>La carga es EAGER porque el menu se arma justo al iniciar sesion, en
     * cuanto se resuelve el rol del usuario: dejarla perezosa obligaria a
     * mantener abierta la sesion de Hibernate hasta ese punto.</p>
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "rol_vista",
        joinColumns = @JoinColumn(name = "fn_rol_id"),
        inverseJoinColumns = @JoinColumn(name = "fn_vista_id")
    )
    private List<Vista> vistas;
}