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
 * <p>El proyecto autentica emitiendo el JWT directamente en
 * {@code AuthService}, por lo que esta clase no interviene en el login. Se
 * incluye porque el estandar la exige y porque es el punto de integracion
 * necesario si mas adelante se adopta el {@code AuthenticationManager} de
 * Spring, se anaden proveedores adicionales o se usa {@code @PreAuthorize}
 * con expresiones que consulten el {@code UserDetails}.</p>
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
                        // Mensaje generico a proposito: no revela si la cuenta existe.
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
