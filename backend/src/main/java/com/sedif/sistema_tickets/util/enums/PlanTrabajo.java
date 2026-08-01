package com.sedif.sistema_tickets.util.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Metas del plan anual de trabajo del area de TI.
 *
 * <p>Cada actividad extra y cada resolucion de ticket puede imputarse a una
 * meta, para poder medir el avance al cierre del ano.</p>
 *
 * <p><b>PLANTILLA — pendiente de ajustar.</b> Las claves numericas se tomaron
 * de los datos ya registrados en las tablas {@code actividad_extra} y
 * {@code ticket} (aparecen las claves 1, 2, 5, 6, 7, 8 y 11), pero las
 * descripciones son genericas porque el listado real de metas no consta en el
 * codigo ni en la base de datos.</p>
 *
 * <p>Para adaptarlo: sustituir el texto de cada {@code descripcion} por el
 * nombre real de la meta. <b>No cambiar las claves numericas</b>, porque son
 * las que ya estan guardadas en los registros historicos.</p>
 */
public enum PlanTrabajo {

    META_1(1, "Meta 1 — pendiente de definir"),
    META_2(2, "Meta 2 — pendiente de definir"),
    META_3(3, "Meta 3 — pendiente de definir"),
    META_4(4, "Meta 4 — pendiente de definir"),
    META_5(5, "Meta 5 — pendiente de definir"),
    META_6(6, "Meta 6 — pendiente de definir"),
    META_7(7, "Meta 7 — pendiente de definir"),
    META_8(8, "Meta 8 — pendiente de definir"),
    META_9(9, "Meta 9 — pendiente de definir"),
    META_10(10, "Meta 10 — pendiente de definir"),
    META_11(11, "Meta 11 — pendiente de definir"),
    META_12(12, "Meta 12 — pendiente de definir");

    private final int clave;
    private final String descripcion;

    PlanTrabajo(int clave, String descripcion) {
        this.clave = clave;
        this.descripcion = descripcion;
    }

    public int getClave() {
        return clave;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Busca la meta por su clave numerica.
     *
     * <p>Devuelve {@link Optional#empty()} en lugar de lanzar excepcion: los
     * registros historicos pueden contener claves que ya no existan, y eso no
     * debe romper la consulta de un listado.</p>
     */
    public static Optional<PlanTrabajo> porClave(Integer clave) {
        if (clave == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(m -> m.clave == clave)
                .findFirst();
    }

    /** Indica si una clave corresponde a una meta conocida. */
    public static boolean esClaveValida(Integer clave) {
        return porClave(clave).isPresent();
    }
}
