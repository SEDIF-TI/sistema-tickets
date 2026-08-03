package com.sedif.sistema_tickets.core.estatusticket;

import com.sedif.sistema_tickets.exception.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catalogo de estatus de ticket.
 *
 * <p>La version anterior colgaba de {@code /api/estatus}, ruta que no encajaba
 * con ningun patron protegido de {@code SecurityConfig}: bastaba estar
 * autenticado para crear estatus, de modo que cualquier empleado podia alterar
 * el catalogo del que depende todo el flujo de tickets. Ahora la ruta vive
 * bajo {@code /api/v1/admin} y exige rol ADMINISTRADOR.</p>
 */
@RestController
@RequestMapping("/api/v1/admin/estatus")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class EstatusResource {

    private final EstatusService estatusService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EstatusRecord>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok(estatusService.listar()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EstatusRecord>> crearEstatus(
            @Valid @RequestBody EstatusRecord record) {
        EstatusRecord creado = estatusService.crearEstatus(record);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(creado, "Estatus creado correctamente."));
    }
}
