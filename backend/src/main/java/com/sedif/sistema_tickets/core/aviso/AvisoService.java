package com.sedif.sistema_tickets.core.aviso;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AvisoService {

    private final AvisoRepository avisoRepository;

    @Transactional
    public AvisoResponseRecord crearAvisoGlobal(AvisoRequestRecord request) {
        if (request == null || request.titulo() == null || request.titulo().isBlank()) {
            throw new IllegalArgumentException("El título del aviso es obligatorio.");
        }
        if (request.mensaje() == null || request.mensaje().isBlank()) {
            throw new IllegalArgumentException("El mensaje del aviso es obligatorio.");
        }

        Aviso nuevoAviso = new Aviso();
        nuevoAviso.setTitulo(request.titulo());
        nuevoAviso.setMensaje(request.mensaje());
        
        // Si el frontend manda el estado activo, lo usamos; si no, true por defecto
        nuevoAviso.setActivo(request.activo() != null ? request.activo() : true);
        nuevoAviso.setAreaId(request.areaId());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String usuarioActual = authentication.getName();
            nuevoAviso.setCreadoPor(usuarioActual);
            nuevoAviso.setModificadoPor(usuarioActual);
        }

        Aviso avisoGuardado = avisoRepository.save(nuevoAviso);
        return AvisoResponseRecord.desdeEntidad(avisoGuardado);
    }

    @Transactional(readOnly = true)
    public List<AvisoResponseRecord> obtenerAvisosActivos() {
        return avisoRepository.findByActivoTrueOrderByIdDesc()
                .stream()
                .map(AvisoResponseRecord::desdeEntidad)
                .toList();
    }

    // ---> NUEVO: Obtener todos para el admin
    @Transactional(readOnly = true)
    public List<AvisoResponseRecord> obtenerTodos() {
        return avisoRepository.findAllByOrderByIdDesc()
                .stream()
                .map(AvisoResponseRecord::desdeEntidad)
                .toList();
    }

    // ---> NUEVO: Actualizar aviso (Para el interruptor)
    @Transactional
    public AvisoResponseRecord actualizarAviso(Long id, AvisoRequestRecord request) {
        Aviso aviso = avisoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Aviso no encontrado"));
        
        // Aquí está el cambio: el backend ahora acepta y guarda el booleano y el área
        aviso.setTitulo(request.titulo());
        aviso.setMensaje(request.mensaje());
        if (request.activo() != null) aviso.setActivo(request.activo());
        aviso.setAreaId(request.areaId()); // Asegúrate de tener este setter en Aviso.java
        
        return AvisoResponseRecord.desdeEntidad(avisoRepository.save(aviso));
    }

    // ---> NUEVO: Eliminar físico de la base de datos
    @Transactional
    public void eliminarAvisoFisico(Long id) {
        if (!avisoRepository.existsById(id)) {
            throw new IllegalArgumentException("Aviso no encontrado");
        }
        avisoRepository.deleteById(id);
    }
}