package com.sedif.sistema_tickets.core.area;

import com.sedif.sistema_tickets.core.usuarios.Usuario;
import com.sedif.sistema_tickets.core.usuarios.UsuarioRepository;
import com.sedif.sistema_tickets.exception.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Gestion del catalogo de areas.
 *
 * <p>Mantiene dos invariantes. El nombre no se repite, porque identifica al
 * area en todos los selectores; y un area con personal activo no puede darse
 * de baja, ya que sus usuarios quedarian sin area valida.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AreaService {

    private final AreaRepository areaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public AreaResponse crearArea(AreaRecord record) {
        String nombre = record.nombre().trim();

        // Dos areas con el mismo nombre son indistinguibles en los selectores
        // y reparten al personal entre ambas sin que nadie lo note.
        if (areaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new IllegalArgumentException("Ya existe un area con el nombre " + nombre + ".");
        }

        Area nuevaArea = new Area();
        nuevaArea.setNombre(nombre);
        nuevaArea.setActivo(record.activo() != null ? record.activo() : true);
        nuevaArea.setPrioritaria(record.prioritaria() != null ? record.prioritaria() : false);

        return AreaResponse.desdeEntidad(areaRepository.save(nuevaArea));
    }

    @Transactional(readOnly = true)
    public List<AreaResponse> listarAreas() {
        return areaRepository.findAll()
            .stream()
            .map(AreaResponse::desdeEntidad)
            .toList();
    }

    /** Campos por los que se admite ordenar el listado de areas. */
    private static final Set<String> CAMPOS_ORDENABLES = Set.of("id", "nombre", "activo", "prioritaria");

    /**
     * Listado paginado del panel, con busqueda y filtro de estado resueltos en
     * la base de datos.
     */
    @Transactional(readOnly = true)
    public PageResponse<AreaResponse> listarAreasPaginado(
            String busqueda, Boolean activo, Pageable pageable) {

        return PageResponse.de(
                areaRepository.buscarPaginado(normalizarBusqueda(busqueda), activo, sanearOrden(pageable)),
                AreaResponse::desdeEntidad);
    }

    /**
     * Prepara el texto para el LIKE: minusculas, comodines y escape, para que
     * buscar "100%" busque ese literal y no cualquier cosa que empiece por 100.
     */
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

    /**
     * Descarta los campos de ordenacion no permitidos: el {@code Pageable} se
     * arma con lo que llegue en la URL.
     */
    private Pageable sanearOrden(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return pageable;
        }

        List<Sort.Order> permitidos = pageable.getSort().stream()
                .filter(orden -> {
                    boolean valido = CAMPOS_ORDENABLES.contains(orden.getProperty());
                    if (!valido) {
                        log.warn("Orden por campo no permitido: '{}'. Se ignora.", orden.getProperty());
                    }
                    return valido;
                })
                .toList();

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                permitidos.isEmpty() ? Sort.by(Sort.Direction.ASC, "nombre") : Sort.by(permitidos));
    }

    @Transactional
    public AreaResponse actualizarArea(Long id, AreaRecord record) {
        Area areaExistente = areaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("El area indicada no existe."));

        String nombre = record.nombre().trim();
        if (areaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new IllegalArgumentException("Ya existe otra area con el nombre " + nombre + ".");
        }

        areaExistente.setNombre(nombre);

        // Los dos indicadores solo cambian si vienen informados, de modo que
        // una edicion parcial no los reinicie.
        if (record.activo() != null) {
            areaExistente.setActivo(record.activo());
        }

        if (record.prioritaria() != null) {
            areaExistente.setPrioritaria(record.prioritaria());
        }

        Area areaActualizada = areaRepository.save(areaExistente);
        return AreaResponse.desdeEntidad(areaActualizada);
    }

    @Transactional
    public void eliminarArea(Long id) {
        Area areaExistente = areaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("El area indicada no existe."));

        // Dar de baja un area con personal dentro dejaria a esas personas sin
        // area valida, y con ella se rompe el filtro de visibilidad de sus
        // tickets: la bandeja les apareceria vacia sin explicacion.
        long personal = areaRepository.contarUsuariosActivos(id);
        if (personal > 0) {
            throw new IllegalArgumentException(
                    "No se puede dar de baja el area: todavia tiene " + personal
                    + (personal == 1 ? " usuario activo." : " usuarios activos.")
                    + " Reasignalos primero a otra area.");
        }

        // Baja logica: el area desaparece de los selectores pero los tickets
        // historicos conservan su referencia.
        areaExistente.setActivo(false);
        areaRepository.save(areaExistente);
    }

    /**
     * Asigna o retira el tecnico de soporte fijo del area.
     *
     * <p>Con un tecnico asignado, los tickets del area van directos a el en
     * lugar de pasar por el balanceador de carga. Un {@code soporteFijoId}
     * nulo retira la asignacion y devuelve el area al reparto automatico.</p>
     *
     * <p>Solo admite personal con rol SOPORTE y en alta: un tecnico inactivo
     * dejaria los tickets del area sin nadie que los atendiera.</p>
     */
    @Transactional
    public AreaResponse asignarSoporteFijo(Long id, SoporteFijoRequestRecord request) {
        Area area = areaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Área no encontrada."));

        if (request == null || request.soporteFijoId() == null) {
            area.setSoporteFijo(null);
            log.debug("Soporte fijo retirado del area id={}", id);
            return AreaResponse.desdeEntidad(areaRepository.save(area));
        }

        Usuario tecnico = usuarioRepository.findById(request.soporteFijoId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        if (tecnico.getRol() == null || !"SOPORTE".equals(tecnico.getRol().getNombre())) {
            throw new IllegalArgumentException("Violación de integridad: El usuario asignado debe tener el rol de SOPORTE.");
        }


        if (!Boolean.TRUE.equals(tecnico.getActivo())) {
            throw new IllegalArgumentException("Operación denegada: El técnico seleccionado se encuentra inactivo (baja lógica).");
        }

        area.setSoporteFijo(tecnico);
        Area areaActualizada = areaRepository.save(area);

        return AreaResponse.desdeEntidad(areaActualizada);
    }
}