package com.sedif.sistema_tickets.core.usuarios;

import com.sedif.sistema_tickets.core.area.Area;
import com.sedif.sistema_tickets.core.area.AreaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio que contiene la lógica de negocio para la gestión de usuarios.
 */
@Service
@RequiredArgsConstructor 
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final AreaRepository areaRepository;
    private final RolRepository rolRepository; // Inyectamos el nuevo repositorio de Rol

    /**
     * Crea un nuevo usuario en el sistema aplicando reglas de validación.
     */
    @Transactional
    public UsuarioResponse crearUsuario(UsuarioRequest request) {
        // 1. Verificar que el correo no esté duplicado
        if (usuarioRepository.existsByCorreo(request.correo())) {
            throw new IllegalArgumentException("El correo ya está registrado en el sistema.");
        }

        // Buscamos el rol en la base de datos por su ID
        Rol rol = rolRepository.findById(request.rolId())
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado."));

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombre(request.nombre());
        nuevoUsuario.setCorreo(request.correo());
        nuevoUsuario.setPassword(request.password());
        nuevoUsuario.setRol(rol); // Asignamos la entidad Rol

        // 2. Si el nivel de visión del rol es "AREA", es obligatorio asignarle un área
        if ("AREA".equals(rol.getNivelVision())) {
            if (request.areaId() == null) {
                throw new IllegalArgumentException("Un usuario con este rol debe tener un área asignada.");
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

    /**
     * Cambia la disponibilidad de un usuario de soporte.
     */
    @Transactional
    public UsuarioResponse actualizarDisponibilidad(Long id, DisponibilidadRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con el ID: " + id));

        // Validamos comparando el nombre del rol en la entidad
        if (!"SOPORTE".equals(usuario.getRol().getNombre())) {
            throw new IllegalArgumentException("Solo los usuarios con rol de SOPORTE pueden modificar su disponibilidad.");
        }

        usuario.setDisponibleSoporte(request.disponible());
        Usuario usuarioActualizado = usuarioRepository.save(usuario);

        return UsuarioResponse.desdeEntidad(usuarioActualizado);
    }

    /**
     * Realiza la baja lógica de un usuario.
     */
    @Transactional
    public UsuarioResponse inactivarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con el ID: " + id));

        if (!usuario.getActivo()) {
            throw new IllegalArgumentException("El usuario ya se encuentra inactivo.");
        }

        usuario.setActivo(false);

        // Validamos comparando el nombre del rol
        if ("SOPORTE".equals(usuario.getRol().getNombre())) {
            usuario.setDisponibleSoporte(false);
        }

        Usuario usuarioActualizado = usuarioRepository.save(usuario);
        return UsuarioResponse.desdeEntidad(usuarioActualizado);
    }
}