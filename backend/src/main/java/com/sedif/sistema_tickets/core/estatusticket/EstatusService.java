package com.sedif.sistema_tickets.core.estatusticket;

import com.sedif.sistema_tickets.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EstatusService {

    private final EstatusRepository estatusRepository;

    /** Catalogo pequeno y casi inmutable: se cachea para no consultarlo siempre. */
    @Cacheable(CacheConfig.CACHE_ESTATUS)
    @Transactional(readOnly = true)
    public List<EstatusRecord> listar() {
        return estatusRepository.findAll().stream()
                .map(EstatusRecord::desdeEntidad)
                .toList();
    }

    /**
     * Da de alta un estatus.
     *
     * <p>Devuelve un DTO en lugar de la entidad, para no exponer el mapeo JPA
     * a traves de la API. Lanza {@link IllegalArgumentException} en vez de
     * {@code RuntimeException} para que {@code GlobalExceptionHandler} lo
     * traduzca a un 400 en lugar de a un 500.</p>
     */
    @CacheEvict(value = CacheConfig.CACHE_ESTATUS, allEntries = true)
    @Transactional
    public EstatusRecord crearEstatus(EstatusRecord record) {
        String nombreEstatus = record.nombre().trim().toUpperCase();

        if (estatusRepository.findByNombre(nombreEstatus).isPresent()) {
            throw new IllegalArgumentException(
                    "El estatus '" + nombreEstatus + "' ya existe en el sistema.");
        }

        Estatus guardado = estatusRepository.save(new Estatus(nombreEstatus));
        return EstatusRecord.desdeEntidad(guardado);
    }
}
