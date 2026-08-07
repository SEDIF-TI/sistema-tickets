package com.sedif.sistema_tickets.core.usuarios;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sedif.sistema_tickets.core.area.Area;
import com.sedif.sistema_tickets.util.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Usuario del sistema.
 *
 * <p>El acceso se resuelve por correo o por nombre de usuario, de modo que
 * ambos identifican de forma unica. El rol determina los permisos y el menu; el
 * area, la visibilidad de los tickets.</p>
 *
 * <p>Los campos sensibles llevan {@link JsonIgnore} como ultima defensa frente
 * a la fuga de credenciales. Los controladores devuelven DTOs, pero basta con
 * que una entidad relacionada arrastre a esta al serializarse (por ejemplo
 * {@code ActividadExtra.usuario}) para que el hash de la contrasena y el chat
 * de Telegram viajen al navegador. La anotacion solo afecta a la serializacion
 * JSON, no a la persistencia ni a la lectura desde Java.</p>
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

    /** Alternativa al correo para iniciar sesion. Es opcional, pero unico si se define. */
    @Column(name = "s_username", unique = true, length = 50)
    private String username;

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

    /**
     * Marca si un tecnico entra en el reparto automatico de tickets. Solo tiene
     * sentido en el rol SOPORTE y lo gestiona el administrador.
     */
    @Column(name = "b_disponible_soporte", nullable = false)
    private Boolean disponibleSoporte = false;

    /** Area de adscripcion, que acota los tickets que esta persona alcanza a ver. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_area_id")
    private Area area;

    /**
     * Chat de Telegram vinculado, destino de las notificaciones.
     *
     * <p>Lo rellena el bot cuando la persona pulsa el enlace de vinculacion
     * desde su perfil. Nulo significa que no ha vinculado su cuenta y no
     * recibe avisos por ese canal.</p>
     *
     * <p>{@code @JsonIgnore}: es un identificador personal de mensajeria. Con
     * el y el token del bot se pueden enviar mensajes directos a esa persona,
     * asi que no tiene por que viajar al navegador.</p>
     */
    @JsonIgnore
    @Column(name = "telegram_chat_id")
    private Long telegramChatId;

    /**
     * Indica que la clave vigente la genero el sistema. Mientras sea cierto, el
     * frontend obliga a cambiarla antes de dejar usar el resto de pantallas.
     */
    @Column(name = "b_password_temporal", nullable = false)
    private Boolean passwordTemporal = true;
}