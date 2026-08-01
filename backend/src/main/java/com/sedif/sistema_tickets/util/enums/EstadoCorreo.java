package com.sedif.sistema_tickets.util.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Situacion de una cuenta de correo institucional ante el proveedor.
 *
 * <p>Valores tomados de los registros existentes en la tabla
 * {@code correo_institucional}.</p>
 */
public enum EstadoCorreo {

    /** Cuenta operativa. */
    ACTIVO("Activo"),

    /** Cuenta dada de baja ante el proveedor. */
    BAJA("Baja"),

    /** Suspendida temporalmente, sin llegar a darse de baja. */
    SUSPENDIDO("Suspendido");

    private final String etiqueta;

    EstadoCorreo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public static Optional<EstadoCorreo> desde(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        String normalizado = valor.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(e -> e.name().equals(normalizado))
                .findFirst();
    }
}
