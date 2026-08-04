package com.sedif.sistema_tickets.core.equipo;

import com.sedif.sistema_tickets.exception.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Catalogo de equipos.
 *
 * <p>Sirve para no volver a teclear marca y modelo en cada dictamen tecnico:
 * el formulario ofrece autocompletado sobre lo ya registrado.</p>
 *
 * <p>Esta capa no existia: {@code EquipoResource} manipulaba el repositorio
 * directamente desde el controlador, sin transacciones y con la logica de
 * negocio mezclada con la capa web.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EquipoService {

    /** Techo de resultados del autocompletado, para no devolver el catalogo entero. */
    private static final int MAX_SUGERENCIAS = 20;

    private final EquipoRepository equipoRepository;

    /** Sugerencias para el autocompletado del formulario. */
    @Transactional(readOnly = true)
    public List<Equipo> buscar(String termino) {
        if (termino == null || termino.isBlank()) {
            return List.of();
        }
        return equipoRepository.findByDescripcionContainingIgnoreCase(termino.trim())
                .stream()
                .limit(MAX_SUGERENCIAS)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<Equipo> listarPaginado(String busqueda, Pageable pageable) {
        return PageResponse.de(equipoRepository.buscarPaginado(normalizarBusqueda(busqueda), pageable));
    }

    /**
     * Prepara el texto para el LIKE: minusculas, comodines y escape, de modo
     * que buscar "100%" busque ese literal.
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

    @Transactional(readOnly = true)
    public Equipo obtenerPorId(Long id) {
        return equipoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado en el catalogo."));
    }

    /**
     * Alta silenciosa desde el formulario de dictamen: si la descripcion ya
     * existe, actualiza marca y modelo; si no, crea la entrada.
     *
     * <p>Todo se normaliza a mayusculas para evitar duplicados por diferencias
     * de capitalizacion ("Laptop Dell" y "LAPTOP DELL" serian dos entradas
     * distintas en un catalogo pensado justo para reutilizar).</p>
     */
    @Transactional
    public Equipo registrarOActualizar(EquipoRequest peticion) {
        String descripcion = peticion.descripcion().trim().toUpperCase();

        Equipo equipo = equipoRepository.findByDescripcionIgnoreCase(descripcion)
                .orElseGet(() -> {
                    Equipo nuevo = new Equipo();
                    nuevo.setFechaRegistro(LocalDateTime.now());
                    return nuevo;
                });

        equipo.setDescripcion(descripcion);
        if (peticion.marca() != null && !peticion.marca().isBlank()) {
            equipo.setMarca(peticion.marca().trim().toUpperCase());
        }
        if (peticion.modelo() != null && !peticion.modelo().isBlank()) {
            equipo.setModelo(peticion.modelo().trim().toUpperCase());
        }

        return equipoRepository.save(equipo);
    }

    @Transactional
    public Equipo actualizar(Long id, EquipoRequest peticion) {
        Equipo equipo = obtenerPorId(id);

        String descripcion = peticion.descripcion().trim().toUpperCase();

        // La descripcion tiene restriccion UNIQUE. Sin esta comprobacion, el
        // choque lo detectaba la base y devolvia un 500 sin explicar la causa.
        if (equipoRepository.existsByDescripcionIgnoreCaseAndIdNot(descripcion, id)) {
            throw new IllegalArgumentException(
                    "Ya existe otro equipo con la descripcion " + descripcion + ".");
        }

        equipo.setDescripcion(descripcion);
        equipo.setMarca(peticion.marca() != null && !peticion.marca().isBlank()
                ? peticion.marca().trim().toUpperCase() : null);
        equipo.setModelo(peticion.modelo() != null && !peticion.modelo().isBlank()
                ? peticion.modelo().trim().toUpperCase() : null);

        return equipoRepository.save(equipo);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!equipoRepository.existsById(id)) {
            throw new IllegalArgumentException("Equipo no encontrado en el catalogo.");
        }
        equipoRepository.deleteById(id);
        log.info("Equipo id={} eliminado del catalogo.", id);
    }
}
