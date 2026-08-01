package com.sedif.sistema_tickets.util.enums;

/**
 * Roles del sistema.
 *
 * <p>Los roles viven como filas en la tabla {@code rol}, no como enum
 * persistido: este tipo solo sirve para no escribir los nombres a mano en el
 * codigo (por ejemplo en las expresiones {@code @PreAuthorize} o al comparar
 * {@code rol.getNombre()}).</p>
 *
 * <p>Se corrigio el valor {@code AREA}, que no corresponde a ningun rol real:
 * el rol del personal general se llama {@code EMPLEADO}. {@code AREA} es un
 * <b>nivel de vision</b> ({@link NivelVision}), no un rol, y tenerlo aqui
 * inducia a error a quien leyera el enum.</p>
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
}
