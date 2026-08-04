package com.sedif.sistema_tickets.core.taller;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import com.sedif.sistema_tickets.util.enums.EstadoTaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
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
    public com.sedif.sistema_tickets.exception.PageResponse<EquipoReparacionDTO> listarPaginado(
            String busqueda, String estado, org.springframework.data.domain.Pageable pageable) {

        // Un estado desconocido se ignora en lugar de romper la consulta: el
        // filtro es una comodidad de la interfaz, no un dato de negocio.
        String estadoValido = EstadoTaller.desde(estado).map(Enum::name).orElse(null);

        return com.sedif.sistema_tickets.exception.PageResponse.de(
                equipoRepository.buscarPaginado(normalizarBusqueda(busqueda), estadoValido, pageable),
                this::convertirADto);
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
                .orElseThrow(() -> new IllegalArgumentException("No existe un registro de taller con ese identificador."));
        return convertirADto(equipo);
    }

    @Override
    @Transactional
    public EquipoReparacionDTO registrarIngreso(EquipoReparacionRequest request, Long tecnicoId) {
        EquipoReparacion equipo = new EquipoReparacion();
        aplicarDatos(equipo, request);

        equipo.setFolio(generarFolio());
        // Toda alta entra como RECIBIDO; los avances pasan por cambiarEstado.
        equipo.setEstadoTaller(EstadoTaller.RECIBIDO.name());

        if (tecnicoId != null) {
            equipo.setTecnicoAsignado(buscarTecnico(tecnicoId));
        }

        EquipoReparacion guardado = equipoRepository.save(equipo);
        log.info("Equipo ingresado al taller con folio {}", guardado.getFolio());
        return convertirADto(guardado);
    }

    /**
     * Genera el folio del ano en curso.
     *
     * <p>La version anterior usaba {@code count() + 1} sobre toda la tabla, lo
     * que producia folios repetidos si dos tecnicos registraban a la vez, y
     * ademas nunca reiniciaba la numeracion al cambiar de ano. Aqui se toma el
     * maximo consecutivo del ano actual, y la restriccion UNIQUE de la columna
     * es la garantia final frente a una colision.</p>
     */
    private String generarFolio() {
        int anio = Year.now().getValue();
        String prefijo = "REP-" + anio + "-";

        int siguiente = equipoRepository.findMaxConsecutivoDelAnio(prefijo + "%")
                .map(ultimo -> ultimo + 1)
                .orElse(1);

        return prefijo + siguiente;
    }

    private Usuario buscarTecnico(Long tecnicoId) {
        return usuarioRepository.findById(tecnicoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El tecnico indicado no existe."));
    }

    /** Copia los campos editables del DTO a la entidad. */
    private void aplicarDatos(EquipoReparacion equipo, EquipoReparacionRequest r) {
        equipo.setSolicitanteNombre(r.solicitanteNombre());
        equipo.setSolicitanteNumero(r.solicitanteNumero());
        equipo.setDepartamento(r.departamento());

        equipo.setEquipoTipo(r.equipoTipo());
        equipo.setMarca(r.marca());
        equipo.setModelo(r.modelo());
        equipo.setNumeroSerie(r.numeroSerie());
        equipo.setNumeroInventario(r.numeroInventario());

        equipo.setCondicionRecepcion(r.condicionRecepcion());
        equipo.setAccesorios(r.accesorios());
        equipo.setFallaReportada(r.fallaReportada());
        equipo.setDiagnostico(r.diagnostico());
        equipo.setSolucion(r.solucion());

        equipo.setTicketId(r.ticketId());
    }

    @Override
    @Transactional
    public EquipoReparacionDTO actualizar(Long id, EquipoReparacionRequest request, Long tecnicoId) {
        EquipoReparacion existente = equipoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe un registro de taller con ese identificador."));

        // El folio y el estado no se tocan aqui: el folio es inmutable y el
        // estado avanza por su propio endpoint, que valida las transiciones.
        aplicarDatos(existente, request);

        if (tecnicoId != null) {
            existente.setTecnicoAsignado(buscarTecnico(tecnicoId));
        }

        return convertirADto(equipoRepository.save(existente));
    }

    @Override
    @Transactional
    public EquipoReparacionDTO cambiarEstado(Long id, String nuevoEstado) {
        EquipoReparacion existente = equipoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe un registro de taller con ese identificador."));

        // Se valida contra el enum: antes se guardaba cualquier texto en
        // mayusculas, de modo que un error de escritura dejaba el equipo en un
        // estado inexistente e invisible para los filtros del listado.
        EstadoTaller estado = EstadoTaller.desde(nuevoEstado)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El estado '" + nuevoEstado + "' no es valido para un equipo en taller."));

        existente.setEstadoTaller(estado.name());
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