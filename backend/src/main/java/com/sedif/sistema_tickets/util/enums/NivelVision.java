package com.sedif.sistema_tickets.util.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Alcance de los tickets que un rol puede ver.
 *
 * <p>Se guarda como texto en {@code rol.s_nivel_vision} y determina que filtro
 * aplica la bandeja de tickets. Es el mecanismo que evita que el personal de un
 * area vea los reportes de otra.</p>
 *
 * <p>Es un concepto distinto del rol: el rol dice que puede hacer el usuario y
 * el nivel de vision, sobre que tickets. {@link #desde(String)} devuelve
 * {@link Optional#empty()} ante un valor no reconocido, de modo que un nivel
 * mal configurado no puede resolverse concediendo acceso total.</p>
 */
public enum NivelVision {

    /** Todos los tickets de la institucion. Corresponde a ADMINISTRADOR. */
    GLOBAL,

    /** Los del area del usuario. Corresponde a EMPLEADO. */
    AREA,

    /**
     * Los asignados al usuario y los que el mismo levanto.
     * Corresponde a SOPORTE.
     */
    PERSONAL;

    /**
     * Convierte el texto almacenado al enum, normalizando mayusculas y espacios
     * sobrantes. Un valor desconocido devuelve {@link Optional#empty()}.
     */
    public static Optional<NivelVision> desde(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        String normalizado = valor.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(n -> n.name().equals(normalizado))
                .findFirst();
    }
}
