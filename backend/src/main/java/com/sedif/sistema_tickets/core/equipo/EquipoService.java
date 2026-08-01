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
    public PageResponse<Equipo> listarPaginado(Pageable pageable) {
        return PageResponse.de(equipoRepository.findAll(pageable));
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
    public Equipo registrarOActualizar(Equipo peticion) {
        if (peticion.getDescripcion() == null || peticion.getDescripcion().isBlank()) {
            throw new IllegalArgumentException("La descripcion del equipo es obligatoria.");
        }

        String descripcion = peticion.getDescripcion().trim().toUpperCase();

        Equipo equipo = equipoRepository.findByDescripcionIgnoreCase(descripcion)
                .orElseGet(() -> {
                    Equipo nuevo = new Equipo();
                    nuevo.setFechaRegistro(LocalDateTime.now());
                    return nuevo;
                });

        equipo.setDescripcion(descripcion);
        if (peticion.getMarca() != null && !peticion.getMarca().isBlank()) {
            equipo.setMarca(peticion.getMarca().trim().toUpperCase());
        }
        if (peticion.getModelo() != null && !peticion.getModelo().isBlank()) {
            equipo.setModelo(peticion.getModelo().trim().toUpperCase());
        }

        return equipoRepository.save(equipo);
    }

    @Transactional
    public Equipo actualizar(Long id, Equipo peticion) {
        Equipo equipo = obtenerPorId(id);

        if (peticion.getDescripcion() != null && !peticion.getDescripcion().isBlank()) {
            equipo.setDescripcion(peticion.getDescripcion().trim().toUpperCase());
        }
        equipo.setMarca(peticion.getMarca() != null ? peticion.getMarca().trim().toUpperCase() : null);
        equipo.setModelo(peticion.getModelo() != null ? peticion.getModelo().trim().toUpperCase() : null);

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
