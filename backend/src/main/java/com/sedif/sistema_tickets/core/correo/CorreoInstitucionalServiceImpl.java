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
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe un registro de correo con ese identificador."));
    }

    @Override
    @Transactional(readOnly = true)
    public com.sedif.sistema_tickets.exception.PageResponse<CorreoInstitucional> listarPaginado(
            String busqueda, String estado, org.springframework.data.domain.Pageable pageable) {

        // Un estado desconocido se ignora en lugar de romper la consulta.
        String estadoValido = com.sedif.sistema_tickets.util.enums.EstadoCorreo.desde(estado)
                .map(Enum::name)
                .orElse(null);

        return com.sedif.sistema_tickets.exception.PageResponse.de(
                correoRepository.buscarPaginado(normalizarBusqueda(busqueda), estadoValido, pageable));
    }

    /** Prepara el texto para el LIKE: minusculas, comodines y escape. */
    private String normalizarBusqueda(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String escapado = valor.trim().toLowerCase()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escapado + "%";
    }

    @Override
    @Transactional
    public CorreoInstitucional crear(CorreoRequest request) {
        String correoLimpio = request.correo().trim().toLowerCase();

        if (correoRepository.existsByCorreo(correoLimpio)) {
            throw new IllegalArgumentException("El correo '" + correoLimpio + "' ya se encuentra registrado.");
        }

        CorreoInstitucional correo = new CorreoInstitucional();
        aplicarDatos(correo, request, correoLimpio);

        // Toda alta nace activa: el estado solo cambia por su propio endpoint,
        // que es el unico que valida contra el enum EstadoCorreo.
        correo.setEstado(com.sedif.sistema_tickets.util.enums.EstadoCorreo.ACTIVO.name());

        return correoRepository.save(correo);
    }

    /** Vuelca los campos editables. El estado se gestiona aparte a proposito. */
    private void aplicarDatos(CorreoInstitucional destino, CorreoRequest request, String correoLimpio) {
        destino.setNombre(request.nombre().trim());
        destino.setApellidoPaterno(request.apellidoPaterno().trim());
        destino.setApellidoMaterno(vacioANulo(request.apellidoMaterno()));
        destino.setArea(request.area().trim());
        destino.setCargo(vacioANulo(request.cargo()));
        destino.setExtension(vacioANulo(request.extension()));
        destino.setCuotaAlmacenamiento(vacioANulo(request.cuotaAlmacenamiento()));
        destino.setCorreo(correoLimpio);
    }

    private String vacioANulo(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    @Override
    @Transactional
    public CorreoInstitucional actualizar(Long id, CorreoRequest request) {
        CorreoInstitucional existente = obtenerPorId(id);

        String correoLimpio = request.correo().trim().toLowerCase();

        if (correoRepository.existsByCorreoAndIdNot(correoLimpio, id)) {
            throw new IllegalArgumentException("El correo '" + correoLimpio + "' ya pertenece a otro registro.");
        }

        // El estado NO se toca aqui. Antes se copiaba tal cual del cuerpo de la
        // peticion, saltandose el endpoint de cambio de estado —el unico que
        // valida contra el enum—, de modo que una cuenta podia quedar en un
        // estado inexistente e invisible para los filtros del directorio.
        aplicarDatos(existente, request, correoLimpio);

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