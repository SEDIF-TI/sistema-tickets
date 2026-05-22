package com.sedif.sistema_tickets.core.area;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AreaService {

    private final AreaRepository areaRepository;

    // Inyección de dependencias
    public AreaService(AreaRepository areaRepository) {
        this.areaRepository = areaRepository;
    }

    // Método para crear una nueva área
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

    // Método para listar todas las áreas
    public List<Area> listarAreas() {
        return areaRepository.findAll();
    }
}