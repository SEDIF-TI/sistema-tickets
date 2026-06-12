package com.sedif.sistema_tickets.core.usuarios;

public record UsuarioResponse(
        Long id,
        String nombre,
        String correo,
        String rolNombre,
        Boolean activo,
        Boolean disponibleSoporte,
        Long areaId,
        String areaNombre,
        Boolean passwordTemporal,
        String passwordTemporalTexto
) {
    // Método estándar para lectura normal
    public static UsuarioResponse desdeEntidad(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getRol() != null ? usuario.getRol().getNombre() : "SIN ROL",
                usuario.getActivo(),
                usuario.getDisponibleSoporte(),
                usuario.getArea() != null ? usuario.getArea().getId() : null,
                usuario.getArea() != null ? usuario.getArea().getNombre() : "Sin Área asignada",
                usuario.getPasswordTemporal(), // El booleano del estado
                null // Sin texto plano
        );
    }

    // Método especial para la creación de usuarios
    public static UsuarioResponse desdeEntidadConPassword(Usuario usuario, String passwordTemporal) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getRol() != null ? usuario.getRol().getNombre() : "SIN ROL",
                usuario.getActivo(),
                usuario.getDisponibleSoporte(),
                usuario.getArea() != null ? usuario.getArea().getId() : null,
                usuario.getArea() != null ? usuario.getArea().getNombre() : "Sin Área asignada",
                true, // Al crear, forzamos que sea true
                passwordTemporal // Enviamos la contraseña aquí
        );
    }
}