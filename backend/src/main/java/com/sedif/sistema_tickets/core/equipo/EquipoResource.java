package com.sedif.sistema_tickets.core.equipo;

import com.sedif.sistema_tickets.exception.ApiResponse;
import com.sedif.sistema_tickets.exception.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catalogo de equipos para los dictamenes tecnicos.
 *
 * <p>Consultar el catalogo y alimentarlo desde el formulario de dictamen esta
 * al alcance de ADMINISTRADOR y SOPORTE; editar y borrar entradas queda
 * reservado al administrador. Toda la logica vive en {@link EquipoService},
 * que la ejecuta dentro de una transaccion.</p>
 */
@RestController
@RequestMapping("/api/v1/equipos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SOPORTE')")
public class EquipoResource {

    private final EquipoService equipoService;

    /** Autocompletado del formulario de dictamen. */
    @GetMapping("/buscar")
    public ResponseEntity<ApiResponse<List<Equipo>>> buscarEquipos(@RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.ok(equipoService.buscar(q)));
    }

    /** Listado paginado para el panel de administracion del catalogo. */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Equipo>>> listarTodos(
            @RequestParam(required = false) String busqueda,
            @PageableDefault(size = 10, sort = "descripcion", direction = Sort.Direction.ASC)
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok(equipoService.listarPaginado(busqueda, pageable)));
    }

    /**
     * Alta o actualizacion silenciosa desde el formulario de dictamen: evita
     * volver a teclear marca y modelo de equipos que se repiten.
     */
    @PostMapping("/upsert")
    public ResponseEntity<ApiResponse<Equipo>> registrarOActualizar(
            @Valid @RequestBody EquipoRequest equipoRequest) {

        return ResponseEntity.ok(ApiResponse.ok(
                equipoService.registrarOActualizar(equipoRequest), "Catalogo actualizado."));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Equipo>> actualizarEquipo(
            @PathVariable Long id,
            @Valid @RequestBody EquipoRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                equipoService.actualizar(id, request), "Equipo actualizado."));
    }

    /** Borrado del catalogo. Solo ADMINISTRADOR. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> eliminarEquipo(@PathVariable Long id) {
        equipoService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Equipo eliminado del catalogo."));
    }
}
