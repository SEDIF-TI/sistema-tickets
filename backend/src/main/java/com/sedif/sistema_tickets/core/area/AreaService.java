package com.sedif.sistema_tickets.core.area;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AreaService {

    private final AreaRepository areaRepository;
    // Agregamos el repositorio de usuarios necesario para la nueva funcionalidad
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public AreaResponse crearArea(AreaRecord record) {
        Area nuevaArea = new Area();
        nuevaArea.setNombre(record.nombre());
        
        if (record.activo() != null) {
            nuevaArea.setActivo(record.activo());
        } else {
            nuevaArea.setActivo(true);
        }

        Area areaGuardada = areaRepository.save(nuevaArea);
        return AreaResponse.desdeEntidad(areaGuardada);
    }

    @Transactional(readOnly = true)
    public List<AreaResponse> listarAreas() {
        return areaRepository.findAll()
            .stream()
            .map(AreaResponse::desdeEntidad)
            .toList();
    }

    @Transactional
    public AreaResponse actualizarArea(Long id, AreaRecord record) {
        Area areaExistente = areaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("El área con ID " + id + " no existe."));

        areaExistente.setNombre(record.nombre());
        
        if (record.activo() != null) {
            areaExistente.setActivo(record.activo());
        }

        Area areaActualizada = areaRepository.save(areaExistente);
        return AreaResponse.desdeEntidad(areaActualizada);
    }

    @Transactional
    public void eliminarArea(Long id) {
        Area areaExistente = areaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("El área con ID " + id + " no existe."));

        // Borrado lógico
        areaExistente.setActivo(false);
        areaRepository.save(areaExistente);
    }

    // --- NUEVO MÉTODO INTEGRADO ---
    @Transactional
    public AreaResponse asignarSoporteFijo(Long id, SoporteFijoRequestRecord request) {
        // 1. Validación manual estricta
        if (request == null || request.soporteFijoId() == null) {
            throw new IllegalArgumentException("El ID del soporte fijo es obligatorio.");
        }

        // 2. Validar existencia del área
        Area area = areaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Área no encontrada."));

        // 3. Validar existencia del usuario a asignar
        Usuario tecnico = usuarioRepository.findById(request.soporteFijoId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        // 4. Reglas de negocio restrictivas
        if (tecnico.getRol() == null || !"SOPORTE".equals(tecnico.getRol().getNombre())) {
            throw new IllegalArgumentException("Violación de integridad: El usuario asignado debe tener el rol de SOPORTE.");
        }


        if (!Boolean.TRUE.equals(tecnico.getActivo())) {
            throw new IllegalArgumentException("Operación denegada: El técnico seleccionado se encuentra inactivo (baja lógica).");
        }

        // 5. Persistencia
        area.setSoporteFijo(tecnico);
        Area areaActualizada = areaRepository.save(area);

        // 6. Retorno utilizando el mapeo seguro de su compañero
        return AreaResponse.desdeEntidad(areaActualizada);
    }
}