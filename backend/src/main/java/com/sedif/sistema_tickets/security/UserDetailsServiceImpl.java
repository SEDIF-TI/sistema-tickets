package com.sedif.sistema_tickets.security;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Carga los datos del usuario para Spring Security a partir del correo o del
 * nombre de usuario.
 *
 * <p>Traduce un {@code Usuario} del dominio al {@link UserDetails} que Spring
 * Security entiende: el correo como username, el hash BCrypt como credencial,
 * una autoridad {@code ROLE_<ROL>} y la cuenta marcada como deshabilitada
 * cuando el usuario no esta activo.</p>
 *
 * <p>Es el punto de integracion que consulta el {@code AuthenticationManager}
 * de Spring y las expresiones {@code @PreAuthorize} que necesitan resolver un
 * {@code UserDetails}. El login por JWT no pasa por aqui: {@code AuthService}
 * verifica las credenciales y emite el token directamente.</p>
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identificador) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository
                .findByCorreoOrUsername(identificador, identificador)
                .orElseThrow(() -> new UsernameNotFoundException(
                        // Mensaje generico a proposito: distinguir "no existe"
                        // de "contrasena incorrecta" permitiria enumerar cuentas.
                        "Credenciales invalidas."));

        String rol = usuario.getRol() != null ? usuario.getRol().getNombre() : "SIN_ROL";

        return User.builder()
                .username(usuario.getCorreo())
                .password(usuario.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + rol.toUpperCase())))
                .disabled(!Boolean.TRUE.equals(usuario.getActivo()))
                .build();
    }
}
