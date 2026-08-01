package com.sedif.sistema_tickets.util.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Ciclo de vida de un equipo recibido en el taller.
 *
 * <p>Los valores se tomaron del comentario de {@code EquipoReparacion.java} y
 * de los datos ya registrados. Antes el estado era un {@code String} libre que
 * se guardaba con {@code toUpperCase()} sin validar: bastaba un error de
 * escritura para dejar un equipo en un estado inexistente, invisible para
 * cualquier filtro.</p>
 */
public enum EstadoTaller {

    /** El equipo acaba de ingresar; aun no se revisa. */
    RECIBIDO("Recibido"),

    /** Se esta identificando la falla. */
    EN_DIAGNOSTICO("En diagnostico"),

    /** Diagnosticado y en proceso de reparacion. */
    EN_REPARACION("En reparacion"),

    /** Reparado, pendiente de que el usuario lo recoja. */
    REPARADO("Reparado"),

    /** Devuelto a su propietario. Estado final. */
    ENTREGADO("Entregado"),

    /** Sin reparacion posible o no rentable. Estado final. */
    IRREPARABLE("Irreparable");

    private final String etiqueta;

    EstadoTaller(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Texto legible para mostrar en la interfaz. */
    public String getEtiqueta() {
        return etiqueta;
    }

    /** Indica si el equipo ya salio del taller. */
    public boolean esFinal() {
        return this == ENTREGADO || this == IRREPARABLE;
    }

    /**
     * Convierte el texto guardado en base de datos al enum, tolerando
     * mayusculas y espacios.
     *
     * <p>Devuelve {@link Optional#empty()} si el valor no corresponde a
     * ningun estado conocido, para que un dato historico incorrecto no rompa
     * la consulta de un listado completo.</p>
     */
    public static Optional<EstadoTaller> desde(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        String normalizado = valor.trim().toUpperCase().replace(' ', '_');
        return Arrays.stream(values())
                .filter(e -> e.name().equals(normalizado))
                .findFirst();
    }
}
