package com.sedif.sistema_tickets.core.usuarios;

import com.sedif.sistema_tickets.util.enums.RolUsuario;

/**
 * Estructura para devolver los datos del usuario de forma segura (sin contraseñas).
 */
public record UsuarioResponse(
        Long id,
        String nombre,
        String correo,
        RolUsuario rol,
        Boolean activo,
        Boolean disponibleSoporte,
        Long areaId,
        String areaNombre
) {
    /**
     * Transforma una Entidad Usuario en un Record de respuesta.
     */
    public static UsuarioResponse desdeEntidad(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getRol(),
                usuario.getActivo(),
                usuario.getDisponibleSoporte(),
                usuario.getArea() != null ? usuario.getArea().getId() : null,
                usuario.getArea() != null ? usuario.getArea().getNombre() : "Sin Área asignada"
        );
    }
}
