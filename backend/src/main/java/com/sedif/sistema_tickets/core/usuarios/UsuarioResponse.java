package com.sedif.sistema_tickets.core.usuarios;

/**
 * Usuario tal como lo consume el frontend.
 *
 * <p>Resuelve en el servidor los nombres de rol y area, y omite los campos
 * sensibles de la entidad. La contrasena temporal solo se rellena en el alta y
 * en el restablecimiento, mediante {@link #desdeEntidadConPassword}, para que
 * el administrador pueda comunicarsela a la persona.</p>
 */
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
     * <p>Se calcula aqui para que todas las pantallas y documentos muestren la
     * misma forma del nombre sin concatenarlo cada una por su cuenta.</p>
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

    /** Mapeo de lectura: nunca expone contrasena en texto plano. */
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
                usuario.getPasswordTemporal(),
                null
        );
    }

    /**
     * Mapeo del alta y del restablecimiento: acompana la clave recien generada.
     *
     * <p>Es la unica ocasion en que la contrasena viaja en claro, porque el
     * servidor solo guarda su hash y no podra volver a mostrarla.</p>
     */
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
                true,
                passwordTemporal
        );
    }
}