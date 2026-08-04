package com.sedif.sistema_tickets.core.usuarios;

import com.sedif.sistema_tickets.exception.ApiResponse;
import com.sedif.sistema_tickets.exception.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Gestion de usuarios. Todas las operaciones exigen rol ADMINISTRADOR.
 *
 * <p>Los bloques {@code try/catch} que repetia cada metodo desaparecieron:
 * {@code GlobalExceptionHandler} traduce ahora las excepciones a respuestas
 * {@link ApiResponse} con el codigo HTTP correcto.</p>
 *
 * <p>El cambio de contrasena propia ya no vive aqui. Estaba en
 * {@code PUT /api/v1/admin/usuarios/password}, una ruta de administracion que
 * cualquier usuario debia poder invocar, lo que obligaba a abrir una excepcion
 * en {@code SecurityConfig}. Se traslado a {@code PerfilResource}, bajo
 * {@code /api/v1/perfil}.</p>
 */
@RestController
@RequestMapping("/api/v1/admin/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UsuarioResource {

    private final UsuarioService usuarioService;

    /**
     * Listado paginado del panel, con busqueda y filtros por rol y estado
     * resueltos en la base de datos.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UsuarioResponse>>> listarUsuarios(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) Boolean activo,
            @PageableDefault(size = 10, sort = "nombre", direction = Sort.Direction.ASC)
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok(
                usuarioService.listarUsuariosPaginado(busqueda, rol, activo, pageable)));
    }

    /**
     * Catalogo completo sin paginar, para los selectores que necesitan todos
     * los usuarios de golpe.
     */
    @GetMapping("/todos")
    public ResponseEntity<ApiResponse<List<UsuarioResponse>>> listarTodos() {
        return ResponseEntity.ok(ApiResponse.ok(usuarioService.listarUsuarios()));
    }

    /**
     * Personal de soporte, para los selectores de asignacion manual de tickets
     * y de soporte fijo de un area.
     */
    @GetMapping("/soporte")
    public ResponseEntity<ApiResponse<List<UsuarioResponse>>> listarSoporte() {
        return ResponseEntity.ok(ApiResponse.ok(usuarioService.listarSoporte()));
    }

    /**
     * Alta de usuario. La respuesta incluye la contrasena temporal generada
     * para que el administrador pueda comunicarsela a la persona.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UsuarioResponse>> crearUsuario(
            @Valid @RequestBody UsuarioRequest request) {
        UsuarioResponse creado = usuarioService.crearUsuario(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(creado, "Usuario creado correctamente."));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UsuarioResponse>> actualizarUsuario(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarUsuarioRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                usuarioService.actualizarUsuario(id, request), "Usuario actualizado correctamente."));
    }

    /** Genera una nueva contrasena temporal y obliga a cambiarla al entrar. */
    @PutMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<UsuarioResponse>> resetearPassword(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                usuarioService.resetearPassword(id), "Contrasena restablecida correctamente."));
    }

    @PutMapping("/{id}/toggle-estado")
    public ResponseEntity<ApiResponse<UsuarioResponse>> alternarEstado(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                usuarioService.alternarEstadoUsuario(id), "Estado del usuario actualizado."));
    }

    @PatchMapping("/{id}/disponibilidad")
    public ResponseEntity<ApiResponse<UsuarioResponse>> actualizarDisponibilidad(
            @PathVariable Long id,
            @Valid @RequestBody DisponibilidadRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                usuarioService.actualizarDisponibilidad(id, request), "Disponibilidad actualizada."));
    }

    /** Baja logica: el usuario deja de poder entrar, pero se conserva su historial. */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<UsuarioResponse>> inactivarUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                usuarioService.inactivarUsuario(id), "Usuario inactivado correctamente."));
    }
}
