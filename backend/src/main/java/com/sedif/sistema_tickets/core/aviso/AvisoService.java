package com.sedif.sistema_tickets.core.aviso;

import com.sedif.sistema_tickets.core.area.AreaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Alta, edicion y consulta de los avisos que se muestran en los paneles.
 *
 * <p>Un aviso sin area es global y lo ve toda la institucion; con area, solo
 * el personal adscrito a ella. La baja es siempre logica, de modo que el
 * historial de auditoria se conserva.</p>
 */
@Service
@RequiredArgsConstructor
public class AvisoService {

    private final AvisoRepository avisoRepository;
    private final AreaRepository areaRepository;

    @Transactional
    public AvisoResponseRecord crearAvisoGlobal(AvisoRequestRecord request) {
        // El titulo y el mensaje llegan validados por @Valid sobre el DTO.
        verificarAreaExiste(request.areaId());

        Aviso nuevoAviso = new Aviso();
        nuevoAviso.setTitulo(request.titulo().trim());
        nuevoAviso.setMensaje(request.mensaje().trim());
        nuevoAviso.setActivo(request.activo() != null ? request.activo() : true);
        nuevoAviso.setAreaId(request.areaId());
        nuevoAviso.setEliminado(false);

        // La autoria queda registrada en los campos de Auditable.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String usuarioActual = authentication.getName();
            nuevoAviso.setCreadoPor(usuarioActual);
            nuevoAviso.setModificadoPor(usuarioActual);
        }

        Aviso avisoGuardado = avisoRepository.save(nuevoAviso);
        return mapear(avisoGuardado);
    }

    /** Avisos vigentes para la barra de los paneles. */
    @Transactional(readOnly = true)
    public List<AvisoResponseRecord> obtenerAvisosActivos() {
        return avisoRepository.findByActivoTrueAndEliminadoFalseOrderByIdDesc()
                .stream()
                .map(this::mapear)
                .toList();
    }

    /** Listado del panel de administracion: incluye los inactivos. */
    @Transactional(readOnly = true)
    public List<AvisoResponseRecord> obtenerTodos() {
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

    /**
     * Da de baja un aviso.
     *
     * <p>Es una baja logica: marca la fila como eliminada y la apaga a la vez,
     * de modo que no reaparezca si alguien vuelve a activarla.</p>
     */
    @Transactional
    public void eliminarAvisoFisico(Long id) {
        Aviso aviso = avisoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Aviso no encontrado"));
        
        aviso.setEliminado(true);
        aviso.setActivo(false);
        avisoRepository.save(aviso);
    }

    /**
     * Comprueba que el area de destino existe.
     *
     * <p>Un {@code areaId} inexistente produciria un aviso invisible: al tener
     * area no es global, pero ningun usuario pertenece a ella, de modo que no
     * lo veria nadie y tampoco daria error.</p>
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
