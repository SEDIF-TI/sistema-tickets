package com.sedif.sistema_tickets.util.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Metas del plan anual de trabajo del area de TI.
 *
 * <p>Cada actividad extra y cada resolucion de ticket puede imputarse a una
 * meta, para poder medir el avance al cierre del ano.</p>
 *
 * <p>Las claves numericas coinciden con las ya registradas en las tablas
 * {@code actividad_extra} y {@code ticket}. Las descripciones se recuperaron
 * del catalogo que el panel de soporte tenia escrito a mano en el desplegable
 * de resolucion de tickets: el listado vivia duplicado en el frontend y no
 * existia en el backend, asi que cualquier cambio de meta obligaba a tocar el
 * JSX. Ahora el catalogo se sirve desde aqui.</p>
 *
 * <p><b>No cambiar las claves numericas</b>: son las que ya estan guardadas en
 * los registros historicos.</p>
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
