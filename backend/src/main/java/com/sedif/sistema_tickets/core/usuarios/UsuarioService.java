package com.sedif.sistema_tickets.core.usuarios;

import com.sedif.sistema_tickets.core.area.Area;
import com.sedif.sistema_tickets.core.area.AreaRepository;
import com.sedif.sistema_tickets.util.enums.RolUsuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final AreaRepository areaRepository;

    // Inyección de dependencias mediante el constructor
    public UsuarioService(UsuarioRepository usuarioRepository, AreaRepository areaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.areaRepository = areaRepository;
    }

    /**
     * Crea un nuevo usuario en el sistema aplicando reglas de validación.
     */
    @Transactional
    public UsuarioResponse crearUsuario(UsuarioRequest request) {
        // 1. Verificar que el correo no esté duplicado
        if (usuarioRepository.existsByCorreo(request.correo())) {
            throw new IllegalArgumentException("El correo ya está registrado en el sistema.");
        }

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombre(request.nombre());
        nuevoUsuario.setCorreo(request.correo());
        // TODO: En el futuro aquí deberíamos encriptar la contraseña (ej. PasswordEncoder de Spring Security)
        nuevoUsuario.setPassword(request.password());
        nuevoUsuario.setRol(request.rol());

        // 2. Si el rol es AREA, es obligatorio asignarle un área existente
        if (request.rol() == RolUsuario.AREA) {
            if (request.areaId() == null) {
                throw new IllegalArgumentException("Un usuario de tipo AREA debe tener un área asignada.");
            }
            Area area = areaRepository.findById(request.areaId())
                    .orElseThrow(() -> new IllegalArgumentException("El área especificada no existe."));
            nuevoUsuario.setArea(area);
        }

        // 3. Guardar en base de datos
        Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);

        return UsuarioResponse.desdeEntidad(usuarioGuardado);
    }

    /**
     * Obtiene la lista de todos los usuarios registrados.
     */
    public List<UsuarioResponse> listarUsuarios() {
        return usuarioRepository.findAll()
                .stream()
                .map(UsuarioResponse::desdeEntidad)
                .toList();
    }
}
