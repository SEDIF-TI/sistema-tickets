package com.sedif.sistema_tickets.util.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Metas del plan anual de trabajo del area de TI.
 *
 * <p>Cada actividad extra y cada resolucion de ticket puede imputarse a una
 * meta, para poder medir el avance al cierre del ano.</p>
 *
 * <p>Lo que se guarda en las tablas {@code actividad_extra} y {@code ticket} es
 * la clave numerica, no el nombre de la constante; la descripcion acompana a
 * cada meta para poblar el desplegable de resolucion de tickets desde una unica
 * fuente, en lugar de mantener el listado duplicado en el frontend.</p>
 *
 * <p><b>Las claves numericas no deben cambiar</b>: son las que quedan
 * almacenadas en los registros, y alterarlas reasignaria las actividades ya
 * imputadas a metas distintas.</p>
 */
public enum PlanTrabajo {

    META_1(1, "Mantenimiento preventivo equipo oficinas centrales"),
    META_2(2, "Mantenimiento preventivo equipo oficinas metropolitanas"),
    META_3(3, "Mantenimiento preventivo equipo Casa Jóvenes en Progreso"),
    META_4(4, "Mantenimiento preventivo equipo Delegaciones Regionales y Casas Carmen Serdán"),
    META_5(5, "Mantenimiento de Sistemas Institucionales"),
    META_6(6, "Mantenimiento preventivo servidores"),
    META_7(7, "Soporte técnico a equipo de cómputo y software"),
    META_8(8, "Mantenimiento servicio de correo electrónico"),
    META_9(9, "Supervisión servicios de Internet, telefonía IP y correos"),
    META_10(10, "Soporte a videoconferencias"),
    META_11(11, "Realización de respaldos de base de datos (bitácora)"),
    META_12(12, "Mantenimiento e instalación de equipo de CCTV");

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
     * Busca la meta por la clave numerica que se guarda en los registros.
     *
     * <p>Devuelve {@link Optional#empty()} en lugar de lanzar excepcion, tanto
     * ante una clave nula como ante una que no corresponda a ninguna meta: un
     * registro con un valor no reconocido no debe romper la consulta de un
     * listado completo.</p>
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
