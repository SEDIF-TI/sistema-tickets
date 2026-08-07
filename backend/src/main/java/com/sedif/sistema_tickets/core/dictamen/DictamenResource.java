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
 * <p>Solo lectura: la emision vive en {@code /api/v1/documentos/dictamen}, que
 * compone el PDF y registra el documento en la misma operacion.</p>
 *
 * <p>El listado se pagina y se busca en la base, con el orden por fecha de
 * emision descendente por defecto.</p>
 *
 * <p>El {@code @PreAuthorize} de clase restringe el historial a ADMINISTRADOR y
 * SOPORTE: un dictamen recoge datos del servidor publico y de su equipo que no
 * corresponden al resto de roles.</p>
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
