package com.sedif.sistema_tickets.security.auth;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import com.sedif.sistema_tickets.core.usuarios.VistaDTO;
import com.sedif.sistema_tickets.exception.MessageConstants;
import com.sedif.sistema_tickets.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Logica de autenticacion.
 *
 * <p>El metodo {@code registrarUsuario} fue eliminado junto con su endpoint
 * publico: permitia crear cuentas eligiendo el rol desde el cuerpo de la
 * peticion, sin autenticacion previa. El alta de usuarios corresponde a
 * {@code UsuarioService}, accesible solo con rol ADMINISTRADOR.</p>
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    /**
     * Valida las credenciales y emite el access token.
     *
     * <p>Tanto si la cuenta no existe como si la contrasena es incorrecta se
     * devuelve el mismo mensaje generico. Distinguir ambos casos permitiria
     * averiguar que correos estan dados de alta.</p>
     */
    @Transactional(readOnly = true)
    public JwtResponse iniciarSesion(LoginRequest request) {

        // Las restricciones de formato ya las aplico Bean Validation en el
        // controlador (@Valid), asi que aqui solo queda la logica de negocio.
        Usuario usuario = usuarioRepository
                .findByCorreoOrUsername(request.identificador(), request.identificador())
                .orElseThrow(() -> new IllegalArgumentException(MessageConstants.CREDENCIALES_INVALIDAS));

        // La verificacion criptografica va ANTES de comprobar si esta activo:
        // asi la respuesta no revela la existencia de la cuenta a quien no
        // conoce la contrasena.
        if (!passwordEncoder.matches(request.password(), usuario.getPassword())) {
            throw new IllegalArgumentException(MessageConstants.CREDENCIALES_INVALIDAS);
        }

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new IllegalArgumentException(MessageConstants.USUARIO_INACTIVO);
        }

        String token = tokenProvider.generarToken(usuario);

        List<VistaDTO> vistasPermitidas = obtenerVistasPermitidas(usuario);
        Long areaId = usuario.getArea() != null ? usuario.getArea().getId() : null;

        return new JwtResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getRol() != null ? usuario.getRol().getNombre() : null,
                token,
                "Autenticacion exitosa.",
                vistasPermitidas,
                areaId
        );
    }

    /** Vistas activas del menu segun el rol del usuario. */
    /**
     * Vistas del menu que corresponden al rol del usuario.
     *
     * <p>Publico porque tambien lo consulta {@code PerfilResource}: el menu se
     * guardaba en el navegador al iniciar sesion y no se refrescaba nunca, de
     * modo que una vista retirada seguia apareciendo hasta cerrar sesion, y un
     * permiso recien concedido no llegaba al usuario.</p>
     */
    public List<VistaDTO> obtenerVistasPermitidas(Usuario usuario) {
        if (usuario.getRol() == null || usuario.getRol().getVistas() == null) {
            return List.of();
        }
        return usuario.getRol().getVistas().stream()
                .filter(v -> Boolean.TRUE.equals(v.getActivo()))
                .map(v -> new VistaDTO(v.getNombre(), v.getRuta(), v.getIcono()))
                .toList();
    }
}
