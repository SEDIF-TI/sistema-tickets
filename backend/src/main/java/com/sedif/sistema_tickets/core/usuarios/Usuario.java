package com.sedif.sistema_tickets.core.usuarios;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sedif.sistema_tickets.core.area.Area;
import com.sedif.sistema_tickets.util.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa a los usuarios del sistema.
 *
 * <p><b>Ultima linea de defensa contra la fuga de credenciales.</b> Lo
 * correcto es que ningun controlador devuelva esta entidad y use siempre un
 * DTO. Pero basta con que una entidad relacionada la arrastre al serializarse
 * (por ejemplo {@code ActividadExtra.usuario} o
 * {@code EquipoReparacion.tecnicoAsignado}) para que el hash de la contrasena
 * y el chat de Telegram acaben viajando al navegador y siendo visibles en la
 * pestana Network.</p>
 *
 * <p>Los campos sensibles llevan {@link JsonIgnore}: aunque alguien devuelva
 * la entidad por descuido, esos datos no salen. La anotacion no afecta a la
 * persistencia ni a la lectura desde Java, solo a la serializacion JSON.</p>
 */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Long id;

    @Column(name = "s_nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "s_apellido_paterno")
    private String apellidoPaterno;

    @Column(name = "s_apellido_materno")
    private String apellidoMaterno;

    @Column(name = "s_correo", nullable = false, unique = true, length = 100)
    private String correo;

    // --- NUEVO CAMPO AGREGADO PARA LOGIN DUAL ---
    @Column(name = "s_username", unique = true, length = 50)
    private String username; // Campo para login tradicional con username
    // --------------------------------------------

    /**
     * Hash BCrypt de la contrasena.
     *
     * <p>{@code @JsonIgnore} impide que salga en ninguna respuesta JSON. Un
     * hash filtrado permite atacarlo sin limite de intentos y sin dejar rastro
     * en los logs del servidor.</p>
     */
    @JsonIgnore
    @Column(name = "s_password", nullable = false)
    private String password;

    @ManyToOne
    @JoinColumn(name = "fn_rol_id")
    private Rol rol;

    @Column(name = "b_activo", nullable = false)
    private Boolean activo = true;

    // Control de disponibilidad para usuarios con rol SOPORTE gestionado por el Administrador.
    @Column(name = "b_disponible_soporte", nullable = false)
    private Boolean disponibleSoporte = false;

    // Relación muchos a uno: Múltiples usuarios de rol AREA pueden pertenecer a una misma Área.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_area_id")
    private Area area;

    /**
     * Chat de Telegram vinculado, para las notificaciones.
     *
     * <p>{@code @JsonIgnore}: es un identificador personal de mensajeria. Con
     * el y el token del bot se pueden enviar mensajes directos a esa persona,
     * asi que no tiene por que viajar al navegador.</p>
     */
    @JsonIgnore
    @Column(name = "telegram_chat_id")
    private Long telegramChatId;

    // Agrega este campo a tu Usuario.java
    @Column(name = "b_password_temporal", nullable = false)
    private Boolean passwordTemporal = true; // true por defecto al crear
}