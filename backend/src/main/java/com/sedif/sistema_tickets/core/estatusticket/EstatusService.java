package com.sedif.sistema_tickets.core.estatusticket;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EstatusService {

    private final EstatusRepository estatusRepository;

    public Estatus crearEstatus(EstatusRecord record) {
        // Convertimos a mayúsculas para mantener un estándar
        String nombreEstatus = record.nombre().toUpperCase();

        // Validamos que no exista ya un estatus con ese nombre
        if (estatusRepository.findByNombre(nombreEstatus).isPresent()) {
            throw new RuntimeException("El estatus '" + nombreEstatus + "' ya existe en el sistema.");
        }

        Estatus nuevoEstatus = new Estatus(nombreEstatus);
        return estatusRepository.save(nuevoEstatus);
    }
}