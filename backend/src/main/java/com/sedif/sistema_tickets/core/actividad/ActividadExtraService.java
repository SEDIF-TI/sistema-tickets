package com.sedif.sistema_tickets.core.actividad;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import com.sedif.sistema_tickets.exception.MessageConstants;
import com.sedif.sistema_tickets.exception.PageResponse;
import com.sedif.sistema_tickets.util.enums.PlanTrabajo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Actividades del plan anual de trabajo del personal del area.
 *
 * <p>Cada persona registra lo que va realizando frente a las metas fijadas a
 * inicio de ano, de modo que el avance sea medible. Los textos se guardan en
 * mayusculas porque asi se imprimen en el reporte oficial.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ActividadExtraService {

    private final ActividadExtraRepository actividadExtraRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Registra una actividad a nombre del usuario autenticado.
     *
     * @param identificador correo o username tomado del token, NUNCA del
     *                      cuerpo de la peticion: de lo contrario cualquiera
     *                      podria registrar actividades a nombre de otra
     *                      persona.
     */
    @Transactional
    public ActividadExtra registrar(ActividadExtraRequest request, String identificador) {
        Usuario usuario = usuarioRepository.findByCorreoOrUsername(identificador, identificador)
                .orElseThrow(() -> new IllegalArgumentException(MessageConstants.USUARIO_NO_ENCONTRADO));

        // La clave debe corresponder a una meta conocida del plan: de lo
        // contrario el registro no se puede imputar a nada y falsea el avance.
        if (!PlanTrabajo.esClaveValida(request.planTrabajoClave())) {
            throw new IllegalArgumentException(
                    "La meta del plan de trabajo indicada no existe.");
        }

        ActividadExtra actividad = new ActividadExtra();
        actividad.setUsuario(usuario);
        actividad.setPlanTrabajoClave(request.planTrabajoClave());
        actividad.setActividadSolicitada(request.actividadSolicitada().trim().toUpperCase());
        actividad.setSituacionActual(request.situacionActual().trim().toUpperCase());
        actividad.setJustificacion(
                request.justificacion() != null ? request.justificacion().trim().toUpperCase() : null);
        actividad.setFechaActividad(LocalDateTime.now());

        ActividadExtra guardada = actividadExtraRepository.save(actividad);
        log.info("Actividad registrada por el usuario id={} para la meta {}",
                usuario.getId(), request.planTrabajoClave());

        return guardada;
    }

    /** Historial paginado de todas las actividades del area. */
    @Transactional(readOnly = true)
    public PageResponse<ActividadExtra> listarPaginado(Pageable pageable) {
        return PageResponse.de(actividadExtraRepository.findAll(pageable));
    }

    /** Actividades registradas por el usuario autenticado. */
    @Transactional(readOnly = true)
    public PageResponse<ActividadExtra> listarMisActividades(String identificador, Pageable pageable) {
        Usuario usuario = usuarioRepository.findByCorreoOrUsername(identificador, identificador)
                .orElseThrow(() -> new IllegalArgumentException(MessageConstants.USUARIO_NO_ENCONTRADO));

        return PageResponse.de(
                actividadExtraRepository.findByUsuarioId(usuario.getId(), pageable));
    }
}
