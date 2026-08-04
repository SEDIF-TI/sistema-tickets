package com.sedif.sistema_tickets.core.usuarios;

public record UsuarioResponse(
        Long id,
        String nombre,
        String apellidoPaterno,
        String apellidoMaterno,
        /** Nombre y apellidos ya unidos, para tablas y documentos. */
        String nombreCompleto,
        String username,
        String correo,
        /** Id del rol: lo necesita el formulario para preseleccionarlo al editar. */
        Long rolId,
        String rolNombre,
        Boolean activo,
        Boolean disponibleSoporte,
        Long areaId,
        String areaNombre,
        Boolean passwordTemporal,
        String passwordTemporalTexto
) {
    /**
     * Une nombre y apellidos descartando los vacios.
     *
     * <p>Antes cada pantalla los concatenaba por su cuenta, y las que solo
     * leian {@code nombre} mostraban a la persona sin apellidos.</p>
     */
    private static String nombreCompleto(Usuario usuario) {
        return java.util.stream.Stream.of(
                        usuario.getNombre(),
                        usuario.getApellidoPaterno(),
                        usuario.getApellidoMaterno())
                .filter(p -> p != null && !p.isBlank())
                .map(String::trim)
                .collect(java.util.stream.Collectors.joining(" "));
    }

    // Método estándar para lectura normal
    public static UsuarioResponse desdeEntidad(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellidoPaterno(),
                usuario.getApellidoMaterno(),
                nombreCompleto(usuario),
                usuario.getUsername(),
                usuario.getCorreo(),
                usuario.getRol() != null ? usuario.getRol().getId() : null,
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
                usuario.getApellidoPaterno(),
                usuario.getApellidoMaterno(),
                nombreCompleto(usuario),
                usuario.getUsername(),
                usuario.getCorreo(),
                usuario.getRol() != null ? usuario.getRol().getId() : null,
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