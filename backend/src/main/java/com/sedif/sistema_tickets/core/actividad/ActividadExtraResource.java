package com.sedif.sistema_tickets.core.actividad;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/actividades-extras")
@RequiredArgsConstructor
public class ActividadExtraResource {

    private final ActividadExtraRepository actividadExtraRepository;
    private final UsuarioRepository usuarioRepository;

    @PostMapping
    public ResponseEntity<ActividadExtra> registrarActividad(@RequestBody Map<String, Object> payload) {
        
        // 1. Validación de seguridad para que no vuelva a tronar por null
        if (!payload.containsKey("usuarioId") || payload.get("usuarioId") == null) {
            throw new RuntimeException("El ID del usuario no llegó desde el frontend.");
        }

        // Parseamos a String y luego a Long de forma segura
        Long usuarioId = Long.parseLong(payload.get("usuarioId").toString());
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado en la BD"));

        // 2. Llenamos los datos de la actividad
        ActividadExtra actividad = new ActividadExtra();
        actividad.setUsuario(usuario);
        actividad.setPlanTrabajoClave(Integer.parseInt(payload.get("planTrabajoClave").toString()));
        actividad.setActividadSolicitada(payload.get("actividadSolicitada").toString().toUpperCase());
        actividad.setSituacionActual(payload.get("situacionActual").toString().toUpperCase());
        actividad.setJustificacion(payload.get("justificacion").toString().toUpperCase());
        actividad.setFechaActividad(LocalDateTime.now());

        // 3. Guardamos en la base de datos
        ActividadExtra guardada = actividadExtraRepository.save(actividad);
        return ResponseEntity.ok(guardada);
    }
}