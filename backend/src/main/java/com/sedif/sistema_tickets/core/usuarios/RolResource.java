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
 * <p>Endurecido respecto a la version anterior, que tenia dos problemas
 * serios: la ruta {@code /api/roles} quedaba fuera del patron
 * {@code /api/v1/admin/**} y por tanto solo exigia estar autenticado, de modo
 * que <b>cualquier empleado podia crear roles</b>; y el {@code POST} recibia
 * la entidad {@link Rol} completa, permitiendo fijar campos arbitrarios
 * (mass assignment).</p>
 *
 * <p>El {@code POST} se elimino: los roles son un catalogo fijo del sistema
 * ({@code ADMINISTRADOR}, {@code SOPORTE}, {@code EMPLEADO}) ligado a la
 * logica de negocio y a las vistas del menu. Crearlos en caliente por HTTP no
 * responde a ninguna necesidad real y era un vector de escalada de
 * privilegios. Si hiciera falta anadir un rol, corresponde a una migracion
 * Flyway revisada.</p>
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
