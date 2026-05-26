package com.sedif.sistema_tickets.core.area;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AreaService {

    private final AreaRepository areaRepository;

    /**
     * Crea una nueva área en el sistema.
     */
    public Area crearArea(AreaRecord record) {
        Area nuevaArea = new Area();
        nuevaArea.setNombre(record.nombre());
        
        if (record.activo() != null) {
            nuevaArea.setActivo(record.activo());
        } else {
            nuevaArea.setActivo(true);
        }

        return areaRepository.save(nuevaArea);
    }

    /**
     * Obtiene la lista de todas las áreas registradas.
     */
    public List<Area> listarAreas() {
        return areaRepository.findAll();
    }

    /**
     * Actualiza los datos de un área existente.
     */
    public Area actualizarArea(Long id, AreaRecord record) {
        Area areaExistente = areaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("El área con ID " + id + " no existe."));

        areaExistente.setNombre(record.nombre());
        
        if (record.activo() != null) {
            areaExistente.setActivo(record.activo());
        }

        return areaRepository.save(areaExistente);
    }

    /**
     * Realiza un borrado lógico del área (cambia b_activo a false).
     */
    public void eliminarArea(Long id) {
        Area areaExistente = areaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("El área con ID " + id + " no existe."));

        areaExistente.setActivo(false);
        areaRepository.save(areaExistente);
    }
}