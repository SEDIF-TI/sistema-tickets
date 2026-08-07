package com.sedif.sistema_tickets.core.usuarios;

/**
 * DTO de salida del catalogo de roles.
 *
 * <p>Se serializa en lugar de la entidad {@link Rol}, que arrastra la
 * coleccion EAGER de vistas y expondria la estructura interna del sistema de
 * permisos.</p>
 */
public record RolRecord(
        Long id,
        String nombre,
        String nivelVision
) {

    public static RolRecord desdeEntidad(Rol rol) {
        return new RolRecord(rol.getId(), rol.getNombre(), rol.getNivelVision());
    }
}
