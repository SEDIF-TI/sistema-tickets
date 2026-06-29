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
        nuevoAviso.setActivo(request.activo() != null ? request.activo() : true);
        nuevoAviso.setAreaId(request.areaId());
        nuevoAviso.setEliminado(false); // Por defecto al crear no está eliminado

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
        // Ahora llama al método que excluye a los eliminados
        return avisoRepository.findByActivoTrueAndEliminadoFalseOrderByIdDesc()
                .stream()
                .map(AvisoResponseRecord::desdeEntidad)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AvisoResponseRecord> obtenerTodos() {
        // Ahora llama al método que excluye a los eliminados
        return avisoRepository.findByEliminadoFalseOrderByIdDesc()
                .stream()
                .map(AvisoResponseRecord::desdeEntidad)
                .toList();
    }

    @Transactional
    public AvisoResponseRecord actualizarAviso(Long id, AvisoRequestRecord request) {
        Aviso aviso = avisoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Aviso no encontrado"));
        
        aviso.setTitulo(request.titulo());
        aviso.setMensaje(request.mensaje());
        if (request.activo() != null) aviso.setActivo(request.activo());
        aviso.setAreaId(request.areaId()); 
        
        return AvisoResponseRecord.desdeEntidad(avisoRepository.save(aviso));
    }

    // ---> CORRECCIÓN: Ahora aplica Baja Lógica en lugar de borrado físico
    @Transactional
    public void eliminarAvisoFisico(Long id) {
        Aviso aviso = avisoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Aviso no encontrado"));
        
        aviso.setEliminado(true);
        aviso.setActivo(false); // También lo apagamos por seguridad
        avisoRepository.save(aviso);
    }
}