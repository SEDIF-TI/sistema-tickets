package com.sedif.sistema_tickets.core.usuarios;

public record UsuarioResponse(
        Long id,
        String nombre,
        String correo,
        String rolNombre, // Cambiado a String
        Boolean activo,
        Boolean disponibleSoporte,
        Long areaId,
        String areaNombre
) {
    public static UsuarioResponse desdeEntidad(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getRol() != null ? usuario.getRol().getNombre() : "SIN ROL",
                usuario.getActivo(),
                usuario.getDisponibleSoporte(),
                usuario.getArea() != null ? usuario.getArea().getId() : null,
                usuario.getArea() != null ? usuario.getArea().getNombre() : "Sin Área asignada"
        );
    }
}