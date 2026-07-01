package com.sedif.sistema_tickets.core.auth;

import com.sedif.sistema_tickets.core.usuarios.Rol;
import com.sedif.sistema_tickets.core.usuarios.RolRepository;
import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sedif.sistema_tickets.core.usuarios.VistaDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository; 
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public AuthResponseRecord iniciarSesion(LoginRequestRecord request) {
        
        // 1. Validación manual estricta
        if (request == null || request.identificador() == null || request.identificador().isBlank()) {
            throw new IllegalArgumentException("El correo electrónico o usuario es obligatorio.");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria.");
        }

        // 2. Búsqueda por correo o username
        Usuario usuario = usuarioRepository.findByCorreoOrUsername(request.identificador(), request.identificador())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas."));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new IllegalArgumentException("El usuario se encuentra inactivo. Contacte al administrador.");
        }

        // 3. Verificación criptográfica
        if (!passwordEncoder.matches(request.password(), usuario.getPassword())) {
            throw new IllegalArgumentException("Credenciales inválidas.");
        }

        // 4. Generación del JWT en memoria
        String tokenJwt = jwtService.generarToken(usuario);

        // Transformar las Vistas de la base de datos a VistaDTO
        List<VistaDTO> vistasPermitidas = new ArrayList<>();
    
        if (usuario.getRol() != null && usuario.getRol().getVistas() != null) {
            vistasPermitidas = usuario.getRol().getVistas().stream()
                    .filter(v -> Boolean.TRUE.equals(v.getActivo()))
                    .map(v -> new VistaDTO(v.getNombre(), v.getRuta(), v.getIcono()))
                    .collect(Collectors.toList());
        }

        // CORRECCIÓN CRÍTICA: Extraer el ID desde el objeto completo 'area' de forma segura
        Long idDelArea = (usuario.getArea() != null) ? usuario.getArea().getId() : null;

        // Construimos el nombre completo para el frontend
        String nombreCompleto = usuario.getNombre() + " " + 
                                (usuario.getApellidoPaterno() != null ? usuario.getApellidoPaterno() : "") + " " + 
                                (usuario.getApellidoMaterno() != null ? usuario.getApellidoMaterno() : "");

        return new AuthResponseRecord(
                usuario.getId(),
                nombreCompleto.trim(), 
                usuario.getRol().getNombre(),
                tokenJwt,
                "Autenticación exitosa.",
                vistasPermitidas, 
                idDelArea,
                // Le pasamos el valor exacto de la base de datos (o false si es nulo)
                usuario.getPasswordTemporal() != null ? usuario.getPasswordTemporal() : false 
        );
    }

    public String registrarUsuario(RegistroRequest request) {
        if (usuarioRepository.findByCorreoOrUsername(request.correo(), request.username()).isPresent()) {
            throw new IllegalArgumentException("Error: El correo o nombre de usuario ya están registrados.");
        }

        Rol rol = rolRepository.findById(request.rolId())
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado con ID: " + request.rolId()));

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombre(request.nombre());
        nuevoUsuario.setApellidoPaterno(request.apellidoPaterno());
        nuevoUsuario.setApellidoMaterno(request.apellidoMaterno());
        nuevoUsuario.setCorreo(request.correo());
        nuevoUsuario.setUsername(request.username());
        nuevoUsuario.setPassword(passwordEncoder.encode(request.password()));
        
        nuevoUsuario.setRol(rol); 
        nuevoUsuario.setActivo(true);
        nuevoUsuario.setDisponibleSoporte(false);
        
        // ---> LÍNEA CLAVE QUE ACTIVA LA REDIRECCIÓN EN EL FRONTEND <---
        nuevoUsuario.setPasswordTemporal(true);

        usuarioRepository.save(nuevoUsuario);
        return "Usuario registrado exitosamente.";
    }
}