package com.sedif.sistema_tickets.util.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Alcance de los tickets que un rol puede ver.
 *
 * <p>Se guarda como texto en {@code rol.s_nivel_vision} y determina que
 * devuelve la bandeja de tickets. Es el mecanismo que evita que el personal de
 * un area vea los reportes de otra.</p>
 *
 * <p>Antes este concepto solo existia como cadena suelta comparada a mano, y
 * un valor no reconocido acababa concediendo acceso a todos los tickets.
 * Ahora un nivel desconocido se rechaza de forma explicita.</p>
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
