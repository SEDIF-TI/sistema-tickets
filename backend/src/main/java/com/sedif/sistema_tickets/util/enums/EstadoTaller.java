package com.sedif.sistema_tickets.util.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Ciclo de vida de un equipo recibido en el taller.
 *
 * <p>Acota los valores que puede tomar el estado de una reparacion, que en la
 * base de datos es una columna de texto: sin este enum, un error de escritura
 * dejaria el equipo en un estado inexistente e invisible para los filtros. El
 * nombre de la constante es lo que se almacena, y la etiqueta asociada, lo que
 * se muestra en la interfaz.</p>
 *
 * <p>El recorrido normal va de {@code RECIBIDO} a {@code ENTREGADO} pasando por
 * diagnostico y reparacion; {@code IRREPARABLE} es la salida alternativa
 * cuando la reparacion no es posible o no compensa.</p>
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

    /** Indica si el equipo ya salio del taller, por entrega o por descarte. */
    public boolean esFinal() {
        return this == ENTREGADO || this == IRREPARABLE;
    }

    /**
     * Convierte al enum el texto guardado en base de datos, normalizando
     * mayusculas, espacios sobrantes y el espacio interior de los nombres
     * compuestos.
     *
     * <p>Devuelve {@link Optional#empty()} si el valor no corresponde a ningun
     * estado conocido, para que un registro con un dato incorrecto no rompa la
     * consulta de un listado completo.</p>
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
