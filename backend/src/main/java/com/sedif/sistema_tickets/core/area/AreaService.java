package com.sedif.sistema_tickets.core.area;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio que contiene la lógica de negocio para la gestión de áreas.
 */
@Service
@RequiredArgsConstructor // Lombok: Genera automáticamente el constructor para inyectar AreaRepository
public class AreaService {

    private final AreaRepository areaRepository;

    // Se eliminó el constructor manual public AreaService(...)

    /**
     * Crea una nueva área en el sistema.
     */
    public Area crearArea(AreaRecord record) {
        Area nuevaArea = new Area();
        nuevaArea.setNombre(record.nombre());
        
        // Si no mandan el estatus activo, por defecto es true
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
}