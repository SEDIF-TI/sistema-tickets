package com.sedif.sistema_tickets.core.aviso;

import com.sedif.sistema_tickets.exception.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Gestion y consulta de avisos.
 *
 * <p>Antes ninguna operacion comprobaba el rol: al colgar de
 * {@code /api/v1/avisos} quedaba fuera del patron {@code /api/v1/admin/**} y
 * bastaba con estar autenticado, de modo que <b>cualquier empleado podia
 * crear, editar o borrar los avisos globales</b> que ve toda la organizacion.</p>
 *
 * <p>La ruta se mantiene por compatibilidad con el frontend, pero la
 * autorizacion se aplica ahora por metodo: solo la lectura de avisos activos
 * queda abierta a cualquier usuario autenticado, porque la necesitan todos los
 * paneles ({@code MainLayout} y {@code SoporteLayout}).</p>
 */
@RestController
@RequestMapping("/api/v1/avisos")
@RequiredArgsConstructor
public class AvisoResource {

    private final AvisoService avisoService;

    /** Avisos vigentes para el panel del usuario. Accesible a cualquier sesion. */
    @GetMapping("/activos")
    public ResponseEntity<ApiResponse<List<AvisoResponseRecord>>> listarAvisosActivos() {
        return ResponseEntity.ok(ApiResponse.ok(avisoService.obtenerAvisosActivos()));
    }

    /** Listado completo, incluidos inactivos: solo para el panel de administracion. */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<List<AvisoResponseRecord>>> listarTodos() {
        return ResponseEntity.ok(ApiResponse.ok(avisoService.obtenerTodos()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<AvisoResponseRecord>> crearAviso(
            @Valid @RequestBody AvisoRequestRecord request) {
        AvisoResponseRecord creado = avisoService.crearAvisoGlobal(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(creado, "Aviso creado correctamente."));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<AvisoResponseRecord>> actualizarAviso(
            @PathVariable Long id,
            @Valid @RequestBody AvisoRequestRecord request) {
        return ResponseEntity.ok(ApiResponse.ok(
                avisoService.actualizarAviso(id, request), "Aviso actualizado correctamente."));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> eliminarAviso(@PathVariable Long id) {
        avisoService.eliminarAvisoFisico(id);
        return ResponseEntity.ok(ApiResponse.ok("Aviso eliminado correctamente."));
    }
}
