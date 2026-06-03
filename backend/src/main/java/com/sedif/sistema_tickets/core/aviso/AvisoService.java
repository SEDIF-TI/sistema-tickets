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
        // Validación manual estricta
        if (request == null || request.titulo() == null || request.titulo().isBlank()) {
            throw new IllegalArgumentException("El título del aviso es obligatorio.");
        }
        if (request.mensaje() == null || request.mensaje().isBlank()) {
            throw new IllegalArgumentException("El mensaje del aviso es obligatorio.");
        }

        Aviso nuevoAviso = new Aviso();
        nuevoAviso.setTitulo(request.titulo());
        nuevoAviso.setMensaje(request.mensaje());
        // 'activo' ya es true por defecto en la entidad

        // ----------------------------------------------------------------------
        // NUEVO: EXTRACCIÓN DEL USUARIO DESDE EL CONTEXTO DE SEGURIDAD JWT
        // ----------------------------------------------------------------------
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // Verificamos que exista una autenticación activa (que haya pasado el filtro)
        if (authentication != null && authentication.isAuthenticated()) {
            // Extraemos el identificador (correo o username) que guardamos en el token
            String usuarioActual = authentication.getName();
            
            // Asumiendo que su clase Auditable tiene estos setters expuestos:
            nuevoAviso.setCreadoPor(usuarioActual);
            nuevoAviso.setModificadoPor(usuarioActual);
        }
        // ----------------------------------------------------------------------

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
}