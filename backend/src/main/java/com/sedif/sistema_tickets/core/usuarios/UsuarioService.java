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
}