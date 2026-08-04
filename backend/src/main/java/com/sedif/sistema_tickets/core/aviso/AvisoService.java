package com.sedif.sistema_tickets.core.aviso;

import com.sedif.sistema_tickets.core.area.AreaRepository;
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
    private final AreaRepository areaRepository;

    @Transactional
    public AvisoResponseRecord crearAvisoGlobal(AvisoRequestRecord request) {
        // El titulo y el mensaje los valida @Valid sobre el DTO: la
        // comprobacion manual que habia aqui solo cubria el alta, asi que una
        // edicion podia dejar el aviso en blanco.
        verificarAreaExiste(request.areaId());

        Aviso nuevoAviso = new Aviso();
        nuevoAviso.setTitulo(request.titulo().trim());
        nuevoAviso.setMensaje(request.mensaje().trim());
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
        return mapear(avisoGuardado);
    }

    @Transactional(readOnly = true)
    public List<AvisoResponseRecord> obtenerAvisosActivos() {
        // Ahora llama al método que excluye a los eliminados
        return avisoRepository.findByActivoTrueAndEliminadoFalseOrderByIdDesc()
                .stream()
                .map(this::mapear)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AvisoResponseRecord> obtenerTodos() {
        // Ahora llama al método que excluye a los eliminados
        return avisoRepository.findByEliminadoFalseOrderByIdDesc()
                .stream()
                .map(this::mapear)
                .toList();
    }

    @Transactional
    public AvisoResponseRecord actualizarAviso(Long id, AvisoRequestRecord request) {
        Aviso aviso = avisoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Aviso no encontrado"));
        
        verificarAreaExiste(request.areaId());

        aviso.setTitulo(request.titulo().trim());
        aviso.setMensaje(request.mensaje().trim());
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

    /**
     * Comprueba que el area de destino existe.
     *
     * <p>Sin esta validacion, un areaId inexistente creaba un aviso que no
     * veia nadie: no es global (tiene area) pero ningun usuario pertenece a
     * ella, asi que desaparecia sin error.</p>
     */
    private void verificarAreaExiste(Long areaId) {
        if (areaId != null && !areaRepository.existsById(areaId)) {
            throw new IllegalArgumentException("El area de destino indicada no existe.");
        }
    }

    /** Resuelve el nombre del area para no obligar al cliente a cruzarlo. */
    private AvisoResponseRecord mapear(Aviso aviso) {
        String areaNombre = null;
        if (aviso.getAreaId() != null) {
            areaNombre = areaRepository.findById(aviso.getAreaId())
                    .map(a -> a.getNombre())
                    .orElse("Area no disponible");
        }
        return AvisoResponseRecord.desdeEntidad(aviso, areaNombre);
    }
}
