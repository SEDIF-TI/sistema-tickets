package com.sedif.sistema_tickets.core.usuarios;

import com.sedif.sistema_tickets.core.area.Area;
import com.sedif.sistema_tickets.core.area.AreaRepository;
import com.sedif.sistema_tickets.util.enums.RolUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio que contiene la lógica de negocio para la gestión de usuarios.
 */
@Service
@RequiredArgsConstructor // Lombok: Genera automáticamente el constructor para los repositorios declarados como 'final'
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final AreaRepository areaRepository;

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

    /*
     * Cambia la disponibilidad de un usuario de soporte.
     */
    
    @Transactional
    public UsuarioResponse actualizarDisponibilidad(Long id, DisponibilidadRequest request) {
        // 1. Buscamos al usuario por su ID
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con el ID: " + id));

        // 2. Validamos que el usuario realmente sea de SOPORTE
        if (usuario.getRol() != RolUsuario.SOPORTE) {
            throw new IllegalArgumentException("Solo los usuarios con rol de SOPORTE pueden modificar su disponibilidad.");
        }

        // 3. Actualizamos el estatus y guardamos
        usuario.setDisponibleSoporte(request.disponible());
        Usuario usuarioActualizado = usuarioRepository.save(usuario);

        // 4. Devolvemos el usuario actualizado
        return UsuarioResponse.desdeEntidad(usuarioActualizado);
    }

    /**
     * Realiza la baja lógica de un usuario, cambiando su estado de acceso a inactivo.
     * Si el usuario es de soporte, también revoca su disponibilidad operativa.
     * * @param id Identificador del usuario a inactivar.
     * @return UsuarioResponse con los datos actualizados del usuario.
     * @throws IllegalArgumentException Si el usuario no existe o ya está inactivo.
     */
    @Transactional
    public UsuarioResponse inactivarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con el ID: " + id));

        // Validación: Prevenir redundancia en la base de datos
        if (!usuario.getActivo()) {
            throw new IllegalArgumentException("La operación no es válida. El usuario ya se encuentra inactivo en el sistema.");
        }

        // Se aplica la baja lógica
        usuario.setActivo(false);

        // Regla de negocio de seguridad: Si es soporte, se le retira la disponibilidad
        if (usuario.getRol() == RolUsuario.SOPORTE) {
            usuario.setDisponibleSoporte(false);
        }

        Usuario usuarioActualizado = usuarioRepository.save(usuario);
        return UsuarioResponse.desdeEntidad(usuarioActualizado);
    }

    /**
     * Actualiza la información general de un usuario existente.
     * @param id Identificador del usuario a modificar.
     * @param request DTO con los nuevos datos de actualización.
     * @return UsuarioResponse con los datos actualizados del usuario.
     * @throws IllegalArgumentException Si el usuario o el área no existen, o si hay conflicto de correos.
     */
    @Transactional
    public UsuarioResponse actualizarUsuario(Long id, ActualizarUsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con el ID: " + id));

        // 1. Validación de nombre
        if (request.nombre() != null && !request.nombre().trim().isEmpty()) {
            usuario.setNombre(request.nombre());
        }

        // 2. Validación de correo (Evita colisión con correos de otros usuarios)
        if (request.correo() != null && !request.correo().trim().isEmpty()) {
            // Solo verificamos si el correo entrante es diferente al que ya tiene
            if (!request.correo().equalsIgnoreCase(usuario.getCorreo())) {
                if (usuarioRepository.existsByCorreoAndIdNot(request.correo(), id)) {
                    throw new IllegalArgumentException("El correo electrónico ya está registrado a nombre de otro usuario.");
                }
                usuario.setCorreo(request.correo());
            }
        }

        // 3. Validación de Rol y asignación de Área
        if (request.rol() != null) {
            usuario.setRol(request.rol());
            
            if (request.rol() == RolUsuario.AREA) {
                if (request.areaId() == null) {
                    throw new IllegalArgumentException("Un usuario con rol AREA debe tener un departamento asignado.");
                }
                Area area = areaRepository.findById(request.areaId())
                        .orElseThrow(() -> new IllegalArgumentException("El área especificada no existe con el ID: " + request.areaId()));
                usuario.setArea(area);
            } else {
                // Regla de negocio: Los roles ADMINISTRADOR y SOPORTE no pertenecen a un área regular
                usuario.setArea(null);
            }
        }

        Usuario usuarioActualizado = usuarioRepository.save(usuario);
        return UsuarioResponse.desdeEntidad(usuarioActualizado);
    }

}