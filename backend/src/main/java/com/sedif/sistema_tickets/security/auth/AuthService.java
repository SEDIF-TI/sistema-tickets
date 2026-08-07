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
 * Logica de autenticacion: verifica credenciales y arma la sesion del usuario.
 *
 * <p>Se limita al inicio de sesion. El alta de usuarios corresponde a
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
     * <p>Acepta indistintamente correo o nombre de usuario como identificador.
     * Junto al token devuelve el rol, las vistas del menu y el area, que es lo
     * que el frontend necesita para montar la navegacion sin una segunda
     * llamada.</p>
     *
     * <p>Tanto si la cuenta no existe como si la contrasena es incorrecta se
     * devuelve el mismo mensaje generico. Distinguir ambos casos permitiria
     * averiguar que correos estan dados de alta.</p>
     */
    @Transactional(readOnly = true)
    public JwtResponse iniciarSesion(LoginRequest request) {

        // Bean Validation ya comprobo el formato en el controlador (@Valid),
        // asi que aqui solo queda la logica de negocio.
        Usuario usuario = usuarioRepository
                .findByCorreoOrUsername(request.identificador(), request.identificador())
                .orElseThrow(() -> new IllegalArgumentException(MessageConstants.CREDENCIALES_INVALIDAS));

        // La verificacion criptografica va antes de comprobar si la cuenta esta
        // activa: asi el aviso de usuario inactivo solo lo recibe quien ya
        // demostro conocer la contrasena, y no revela cuentas a nadie mas.
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

    /**
     * Vistas activas del menu que corresponden al rol del usuario.
     *
     * <p>Se descartan las vistas marcadas como inactivas, y un usuario sin rol
     * o con un rol sin vistas obtiene una lista vacia en lugar de un fallo.</p>
     *
     * <p>Es publico porque {@code PerfilResource} lo consulta para refrescar el
     * menu durante la sesion: el frontend lo conserva en el navegador, de modo
     * que sin ese refresco los cambios de permisos no llegarian al usuario
     * hasta que volviera a iniciar sesion.</p>
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
