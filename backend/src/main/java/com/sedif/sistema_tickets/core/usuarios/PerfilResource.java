package com.sedif.sistema_tickets.core.usuarios;

import com.sedif.sistema_tickets.exception.ApiResponse;
import com.sedif.sistema_tickets.exception.MessageConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operaciones que un usuario realiza sobre su propia cuenta.
 *
 * <p>Existe para separar lo personal de lo administrativo. El cambio de
 * contrasena estaba en {@code /api/v1/admin/usuarios/password}: una ruta del
 * area de administracion que, sin embargo, cualquier empleado debia poder
 * invocar. Eso obligaba a abrir una excepcion dentro del bloque protegido de
 * {@code SecurityConfig}, justo el tipo de regla que acaba concediendo mas
 * acceso del previsto.</p>
 *
 * <p>Aqui basta con estar autenticado, y el usuario solo puede actuar sobre su
 * propia cuenta: la identidad se toma del token, nunca de un parametro de la
 * peticion.</p>
 */
@RestController
@RequestMapping("/api/v1/perfil")
@RequiredArgsConstructor
public class PerfilResource {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final com.sedif.sistema_tickets.security.auth.AuthService authService;

    /**
     * Vistas del menu vigentes para el usuario de la sesion.
     *
     * <p>El frontend guarda el menu al iniciar sesion, asi que sin esto una
     * vista retirada seguia dibujandose hasta cerrar sesion —y un permiso
     * recien concedido no aparecia—. La pantalla lo consulta al montarse y
     * actualiza lo que tenga guardado.</p>
     */
    @GetMapping("/vistas")
    public ResponseEntity<ApiResponse<java.util.List<VistaDTO>>> obtenerVistas(
            Authentication authentication) {

        Usuario usuario = usuarioRepository
                .findByCorreoOrUsername(authentication.getName(), authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException(MessageConstants.USUARIO_NO_ENCONTRADO));

        return ResponseEntity.ok(ApiResponse.ok(authService.obtenerVistasPermitidas(usuario)));
    }

    /**
     * Cambia la contrasena del usuario autenticado.
     *
     * <p>El identificador se obtiene de {@link Authentication}, es decir del
     * JWT verificado. Si viniera del cuerpo de la peticion, cualquiera podria
     * cambiar la contrasena de otra persona.</p>
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> cambiarPassword(
            @Valid @RequestBody CambioPasswordRequest request,
            Authentication authentication) {

        usuarioService.actualizarPassword(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.ok(MessageConstants.PASSWORD_ACTUALIZADA));
    }
}
