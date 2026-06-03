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
        if (usuarioRepository.existsByCorreo(request.correo())) {
            throw new IllegalArgumentException("El correo ya está registrado.");
        }

        // 1. Buscamos la entidad Rol, no usamos el Enum
        Rol rol = rolRepository.findById(request.rolId())
                .orElseThrow(() -> new IllegalArgumentException("El rol seleccionado no existe."));

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombre(request.nombre());
        nuevoUsuario.setCorreo(request.correo());
        nuevoUsuario.setPassword(request.password()); 
        nuevoUsuario.setRol(rol);

        // Si el usuario pertenece a una área, asignarla
        if (request.areaId() != null) {
            Area area = areaRepository.findById(request.areaId())
                    .orElseThrow(() -> new IllegalArgumentException("Área no encontrada."));
            nuevoUsuario.setArea(area);
        }

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

    // Asegúrate de que en UsuarioService.java tengas exactamente esto:
    @Transactional
    public UsuarioResponse actualizarUsuario(Long id, ActualizarUsuarioRequest request) {
        // 1. Buscar usuario
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + id));

        // 2. Actualizar campos básicos
        usuario.setNombre(request.nombre());
        usuario.setCorreo(request.correo());

        // 3. Actualizar Rol (buscando la entidad)
        if (request.rolId() != null) {
            Rol rol = rolRepository.findById(request.rolId())
                    .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado."));
            usuario.setRol(rol);
        }

        // 4. Actualizar Área
        if (request.areaId() != null) {
            Area area = areaRepository.findById(request.areaId())
                    .orElseThrow(() -> new IllegalArgumentException("Área no encontrada."));
            usuario.setArea(area);
        }

        return UsuarioResponse.desdeEntidad(usuarioRepository.save(usuario));
    }
}