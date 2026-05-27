package com.sedif.sistema_tickets.core.area;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AreaService {

    private final AreaRepository areaRepository;

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
}