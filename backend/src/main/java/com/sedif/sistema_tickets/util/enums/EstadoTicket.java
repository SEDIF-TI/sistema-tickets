package com.sedif.sistema_tickets.util.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Ciclo de vida de un ticket de soporte.
 *
 * <p>Sustituye a la tabla {@code estadoticket}, que era un catalogo de tres
 * filas que el codigo trataba de todos modos como constantes: las buscaba con
 * {@code findByNombre("ABIERTO")}, {@code findByNombre("EN PROCESO")} y
 * {@code findByNombre("CERRADO")} escritas literalmente. La tabla no aportaba
 * flexibilidad real —anadir una fila no habria creado ninguna transicion
 * nueva, porque las transiciones estan en el codigo— y en cambio si aportaba
 * dos problemas:</p>
 *
 * <ul>
 *   <li>Una base recien migrada arrancaba con el catalogo vacio y el sistema
 *       fallaba al crear el primer ticket, al avisar de que el tecnico va en
 *       camino y al cerrarlo, siempre por un estatus inexistente.</li>
 *   <li>El endpoint de administracion permitia dar de alta estatus nuevos que
 *       ninguna parte del codigo sabia interpretar, dejando tickets en estados
 *       muertos, invisibles para los filtros.</li>
 * </ul>
 *
 * <p>Los nombres de las constantes se guardan tal cual en la columna
 * {@code s_estado} del ticket, y coinciden con los que ya existian en la base
 * de datos salvo por el guion bajo de {@code EN_PROCESO}, que la migracion V4
 * normaliza.</p>
 */
public enum EstadoTicket {

    /** Recien levantado; aun no lo toma ningun tecnico. */
    ABIERTO("Abierto"),

    /** El tecnico aviso que va en camino o ya esta trabajando en el. */
    EN_PROCESO("En proceso"),

    /** Atendido y cerrado. Estado final. */
    CERRADO("Cerrado");

    private final String etiqueta;

    EstadoTicket(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Texto legible para mostrar en la interfaz y en los documentos. */
    public String getEtiqueta() {
        return etiqueta;
    }

    /** Indica si el ticket ya termino su ciclo. */
    public boolean esFinal() {
        return this == CERRADO;
    }

    /**
     * Convierte a enum el texto recibido, tolerando mayusculas, espacios y la
     * forma antigua con espacio ("EN PROCESO" ademas de "EN_PROCESO").
     *
     * <p>Devuelve {@link Optional#empty()} si no corresponde a ningun estado
     * conocido: un filtro con un valor invalido no debe romper la consulta de
     * un listado completo, solo no encontrar nada.</p>
     */
    public static Optional<EstadoTicket> desde(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        String normalizado = valor.trim().toUpperCase().replace(' ', '_');
        return Arrays.stream(values())
                .filter(e -> e.name().equals(normalizado))
                .findFirst();
    }
}
