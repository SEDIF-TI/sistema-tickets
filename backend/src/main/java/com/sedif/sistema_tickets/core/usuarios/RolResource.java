package com.sedif.sistema_tickets.core.usuarios;

import com.sedif.sistema_tickets.exception.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Consulta del catalogo de roles.
 *
 * <p>Solo expone lectura. Los roles ({@code ADMINISTRADOR}, {@code SOPORTE},
 * {@code EMPLEADO}) son un catalogo fijo, ligado a la logica de negocio y a
 * las vistas del menu: darlos de alta en caliente por HTTP seria un vector de
 * escalada de privilegios, de modo que anadir uno corresponde a una migracion
 * Flyway revisada.</p>
 *
 * <p>La ruta cuelga de {@code /api/v1/admin/**} y ademas exige el rol
 * ADMINISTRADOR con {@code @PreAuthorize}, que viaja con el propio controlador
 * aunque cambie el patron de URL.</p>
 */
@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class RolResource {

    private final RolRepository rolRepository;

    /** Lista los roles disponibles para los formularios de administracion. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RolRecord>>> listarRoles() {
        List<RolRecord> roles = rolRepository.findAll().stream()
                .map(RolRecord::desdeEntidad)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(roles));
    }
}
