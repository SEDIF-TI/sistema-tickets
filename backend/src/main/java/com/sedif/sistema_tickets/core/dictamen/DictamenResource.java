package com.sedif.sistema_tickets.core.dictamen;

import com.sedif.sistema_tickets.exception.ApiResponse;
import com.sedif.sistema_tickets.exception.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta del historial de dictamenes tecnicos.
 *
 * <p>La emision sigue estando en {@code /api/v1/documentos/dictamen}, que es
 * donde se compone el PDF y, desde la V10, se registra el documento. Aqui solo
 * se consulta lo ya emitido.</p>
 *
 * <p>Los dictamenes anteriores a la V10 no aparecen porque nunca se guardaron:
 * hasta esa migracion el sistema componia el PDF y lo devolvia sin persistir
 * nada.</p>
 */
@RestController
@RequestMapping("/api/v1/dictamenes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SOPORTE')")
public class DictamenResource {

    private final DictamenService dictamenService;

    /** Historial paginado, ordenado por fecha de emision descendente. */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DictamenResponse>>> listar(
            @RequestParam(required = false) String busqueda,
            @PageableDefault(size = 10, sort = "fechaCreacion", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok(dictamenService.listarPaginado(busqueda, pageable)));
    }

    /** Un dictamen concreto, con todo lo necesario para reimprimirlo. */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DictamenResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(dictamenService.obtener(id)));
    }
}
