package com.sedif.sistema_tickets.core.taller;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EquipoReparacionServiceImpl implements EquipoReparacionService {

    private final EquipoReparacionRepository equipoRepository;
    private final UsuarioRepository usuarioRepository;

    @Autowired
    public EquipoReparacionServiceImpl(EquipoReparacionRepository equipoRepository, UsuarioRepository usuarioRepository) {
        this.equipoRepository = equipoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoReparacionDTO> obtenerTodos() {
        return equipoRepository.findAll().stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoReparacionDTO> buscarPorFiltro(String filtro) {
        if (filtro == null || filtro.trim().isEmpty()) {
            return obtenerTodos();
        }
        return equipoRepository.buscarPorFiltro(filtro.trim()).stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EquipoReparacionDTO obtenerPorId(Long id) {
        EquipoReparacion equipo = equipoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro de taller no encontrado con ID: " + id));
        return convertirADto(equipo);
    }

    @Override
    @Transactional
    public EquipoReparacionDTO registrarIngreso(EquipoReparacion equipo, Long tecnicoId) {
        // Generación de Folio automático (Ej: REP-2026-1)
        long totalRegistros = equipoRepository.count();
        String nuevoFolio = "REP-" + Year.now().getValue() + "-" + (totalRegistros + 1);
        equipo.setFolio(nuevoFolio);
        
        // Estado inicial por defecto
        equipo.setEstadoTaller("RECIBIDO");

        // Asignación de técnico si se proporciona el ID
        if (tecnicoId != null) {
            Usuario tecnico = usuarioRepository.findById(tecnicoId)
                    .orElseThrow(() -> new RuntimeException("Técnico no encontrado."));
            equipo.setTecnicoAsignado(tecnico);
        }

        EquipoReparacion guardado = equipoRepository.save(equipo);
        return convertirADto(guardado);
    }

    @Override
    @Transactional
    public EquipoReparacionDTO actualizar(Long id, EquipoReparacion detalles, Long tecnicoId) {
        EquipoReparacion existente = equipoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro no encontrado."));

        // Actualizamos los datos (excepto el folio que es inmutable)
        existente.setSolicitanteNombre(detalles.getSolicitanteNombre());
        existente.setSolicitanteNumero(detalles.getSolicitanteNumero());
        existente.setDepartamento(detalles.getDepartamento());
        
        existente.setEquipoTipo(detalles.getEquipoTipo());
        existente.setMarca(detalles.getMarca());
        existente.setModelo(detalles.getModelo());
        existente.setNumeroSerie(detalles.getNumeroSerie());
        existente.setNumeroInventario(detalles.getNumeroInventario());
        
        existente.setCondicionRecepcion(detalles.getCondicionRecepcion());
        existente.setAccesorios(detalles.getAccesorios());
        existente.setFallaReportada(detalles.getFallaReportada());
        existente.setDiagnostico(detalles.getDiagnostico());
        existente.setSolucion(detalles.getSolucion());
        
        if (detalles.getEstadoTaller() != null) {
            existente.setEstadoTaller(detalles.getEstadoTaller());
        }

        if (tecnicoId != null) {
            Usuario tecnico = usuarioRepository.findById(tecnicoId)
                    .orElseThrow(() -> new RuntimeException("Técnico no encontrado."));
            existente.setTecnicoAsignado(tecnico);
        }

        return convertirADto(equipoRepository.save(existente));
    }

    @Override
    @Transactional
    public EquipoReparacionDTO cambiarEstado(Long id, String nuevoEstado) {
        EquipoReparacion existente = equipoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro no encontrado."));
        existente.setEstadoTaller(nuevoEstado.toUpperCase());
        return convertirADto(equipoRepository.save(existente));
    }

    // Método utilitario para convertir Entidad a DTO y extraer los datos del Usuario
    private EquipoReparacionDTO convertirADto(EquipoReparacion equipo) {
        EquipoReparacionDTO dto = new EquipoReparacionDTO();
        dto.setId(equipo.getId());
        dto.setFolio(equipo.getFolio());
        dto.setTicketId(equipo.getTicketId());
        
        dto.setSolicitanteNombre(equipo.getSolicitanteNombre());
        dto.setSolicitanteNumero(equipo.getSolicitanteNumero());
        dto.setDepartamento(equipo.getDepartamento());
        
        dto.setEquipoTipo(equipo.getEquipoTipo());
        dto.setMarca(equipo.getMarca());
        dto.setModelo(equipo.getModelo());
        dto.setNumeroSerie(equipo.getNumeroSerie());
        dto.setNumeroInventario(equipo.getNumeroInventario());
        
        dto.setCondicionRecepcion(equipo.getCondicionRecepcion());
        dto.setAccesorios(equipo.getAccesorios());
        dto.setFallaReportada(equipo.getFallaReportada());
        dto.setDiagnostico(equipo.getDiagnostico());
        dto.setSolucion(equipo.getSolucion());
        dto.setEstadoTaller(equipo.getEstadoTaller());
        
        // Mapeo seguro del técnico (Lazy Loading friendly)
        if (equipo.getTecnicoAsignado() != null) {
            dto.setTecnicoAsignadoId(equipo.getTecnicoAsignado().getId());
            // Ajusta el método 'getNombre' según cómo se llame en tu entidad Usuario
            dto.setTecnicoAsignadoNombre(equipo.getTecnicoAsignado().getNombre()); 
        }
        
        return dto;
    }
}