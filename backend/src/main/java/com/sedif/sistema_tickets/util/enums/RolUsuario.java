package com.sedif.sistema_tickets.util.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Roles del sistema.
 *
 * <p>Los roles viven como filas en la tabla {@code rol}, no como enum
 * persistido: este tipo solo sirve para no escribir los nombres a mano en el
 * codigo, por ejemplo al comparar {@code rol.getNombre()} o al redactar las
 * expresiones {@code @PreAuthorize}. Sus constantes deben coincidir con los
 * nombres registrados en esa tabla.</p>
 *
 * <p>El alcance de los tickets que ve cada rol no se decide aqui, sino en
 * {@link NivelVision}, que es un concepto independiente.</p>
 */
public enum RolUsuario {

    /** Acceso total: administracion de usuarios, areas, avisos y metricas. */
    ADMINISTRADOR,

    /** Personal tecnico de TI: atiende tickets, taller, resguardos y correos. */
    SOPORTE,

    /** Personal general: levanta tickets y consulta los de su area. */
    EMPLEADO;

    /** Nombre tal como se guarda en la columna {@code rol.s_nombre}. */
    public String nombreEnBd() {
        return name();
    }

    /**
     * Resuelve el rol a partir de su nombre, tolerando mayusculas, espacios y
     * el prefijo {@code ROLE_} que antepone Spring Security a las autoridades.
     *
     * @return el rol, o vacio si el texto no corresponde a ninguno.
     */
    public static Optional<RolUsuario> desde(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }

        String limpio = valor.trim().toUpperCase().replace("ROLE_", "");
        return Arrays.stream(values())
                .filter(rol -> rol.name().equals(limpio))
                .findFirst();
    }

    /** Indica si el texto recibido corresponde a este rol. */
    public boolean es(String valor) {
        return desde(valor).filter(this::equals).isPresent();
    }
}
