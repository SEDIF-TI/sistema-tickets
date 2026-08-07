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

/**
 * Gestion del taller: ingreso de equipos, folio anual, actualizacion del
 * diagnostico y avance de estado.
 *
 * <p>Todas las lecturas son {@code readOnly}, y la conversion a DTO ocurre
 * dentro de la transaccion: es lo que permite leer el tecnico asignado, que la
 * entidad carga de forma perezosa.</p>
 */
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
        // Toda alta entra como RECIBIDO: los avances pasan por cambiarEstado,
        // que es donde se validan los valores admitidos.
        equipo.setEstadoTaller(EstadoTaller.RECIBIDO.name());

        if (tecnicoId != null) {
            equipo.setTecnicoAsignado(buscarTecnico(tecnicoId));
        }

        EquipoReparacion guardado = equipoRepository.save(equipo);
        log.info("Equipo ingresado al taller con folio {}", guardado.getFolio());
        return convertirADto(guardado);
    }

    /**
     * Genera el folio del ano en curso con el formato {@code REP-<ano>-<n>}.
     *
     * <p>El consecutivo sale del maximo ya emitido ese ano, acotado por el
     * prefijo, de modo que la numeracion arranca de nuevo en cada ejercicio y
     * no depende del total de filas de la tabla. Si el ano no tiene folios
     * todavia, empieza en 1.</p>
     *
     * <p>La restriccion UNIQUE de la columna es la garantia final: dos altas
     * simultaneas pueden leer el mismo maximo, y en ese caso la base rechaza la
     * segunda en lugar de admitir un folio repetido.</p>
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

        // El texto se resuelve contra el enum antes de guardarlo: solo asi el
        // equipo queda en un estado que los filtros del listado reconocen.
        EstadoTaller estado = EstadoTaller.desde(nuevoEstado)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El estado '" + nuevoEstado + "' no es valido para un equipo en taller."));

        existente.setEstadoTaller(estado.name());
        return convertirADto(equipoRepository.save(existente));
    }

    /**
     * Vuelca la entidad al DTO que se expone hacia fuera.
     *
     * <p>Debe invocarse con la transaccion abierta: lee el tecnico asignado,
     * que la entidad carga de forma perezosa.</p>
     */
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
        
        // El tecnico puede no estar asignado todavia, asi que se comprueba
        // antes de tocar la relacion. Se aplana en id y nombre para que la
        // respuesta no arrastre el usuario completo.
        if (equipo.getTecnicoAsignado() != null) {
            dto.setTecnicoAsignadoId(equipo.getTecnicoAsignado().getId());
            dto.setTecnicoAsignadoNombre(equipo.getTecnicoAsignado().getNombre()); 
        }
        
        return dto;
    }
}