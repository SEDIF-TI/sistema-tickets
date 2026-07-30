package com.sedif.sistema_tickets.core.correo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CorreoInstitucionalServiceImpl implements CorreoInstitucionalService {

    private final CorreoInstitucionalRepository correoRepository;

    @Autowired
    public CorreoInstitucionalServiceImpl(CorreoInstitucionalRepository correoRepository) {
        this.correoRepository = correoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CorreoInstitucional> obtenerTodos() {
        return correoRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CorreoInstitucional> buscarPorFiltro(String filtro) {
        if (filtro == null || filtro.trim().isEmpty()) {
            return correoRepository.findAll();
        }
        return correoRepository.buscarPorFiltro(filtro.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CorreoInstitucional> obtenerPorEstado(String estado) {
        return correoRepository.findByEstado(estado.toUpperCase());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CorreoInstitucional> buscarPorFiltroYEstado(String filtro, String estado) {
        boolean tieneFiltro = filtro != null && !filtro.trim().isEmpty();
        boolean tieneEstado = estado != null && !estado.trim().isEmpty();

        if (tieneFiltro && tieneEstado) {
            return correoRepository.buscarPorFiltroYEstado(filtro.trim(), estado.toUpperCase());
        } else if (tieneFiltro) {
            return correoRepository.buscarPorFiltro(filtro.trim());
        } else if (tieneEstado) {
            return correoRepository.findByEstado(estado.toUpperCase());
        } else {
            return correoRepository.findAll();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CorreoInstitucional obtenerPorId(Long id) {
        return correoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Correo institucional no encontrado con ID: " + id));
    }

    @Override
    @Transactional
    public CorreoInstitucional crear(CorreoInstitucional correo) {
        String correoLimpio = correo.getCorreo().trim().toLowerCase();

        if (correoRepository.existsByCorreo(correoLimpio)) {
            throw new IllegalArgumentException("El correo '" + correoLimpio + "' ya se encuentra registrado.");
        }

        correo.setCorreo(correoLimpio);
        if (correo.getEstado() == null || correo.getEstado().isEmpty()) {
            correo.setEstado("ACTIVO");
        }

        return correoRepository.save(correo);
    }

    @Override
    @Transactional
    public CorreoInstitucional actualizar(Long id, CorreoInstitucional datos) {
        CorreoInstitucional existente = obtenerPorId(id);

        String correoLimpio = datos.getCorreo().trim().toLowerCase();

        if (correoRepository.existsByCorreoAndIdNot(correoLimpio, id)) {
            throw new IllegalArgumentException("El correo '" + correoLimpio + "' ya pertenece a otro registro.");
        }

        existente.setNombre(datos.getNombre());
        existente.setApellidoPaterno(datos.getApellidoPaterno());
        existente.setApellidoMaterno(datos.getApellidoMaterno());
        existente.setArea(datos.getArea());
        existente.setCargo(datos.getCargo());
        existente.setExtension(datos.getExtension());
        existente.setCorreo(correoLimpio);
        existente.setEstado(datos.getEstado());
        existente.setCuotaAlmacenamiento(datos.getCuotaAlmacenamiento());

        return correoRepository.save(existente);
    }

    @Override
    @Transactional
    public CorreoInstitucional cambiarEstado(Long id, String nuevoEstado) {
        CorreoInstitucional existente = obtenerPorId(id);
        existente.setEstado(nuevoEstado.toUpperCase());
        return correoRepository.save(existente);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        CorreoInstitucional existente = obtenerPorId(id);
        existente.setEstado("BAJA");
        correoRepository.save(existente);
    }
}