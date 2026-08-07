package com.sedif.sistema_tickets.util.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Ciclo de vida de un ticket de soporte.
 *
 * <p>El conjunto de estados vive en el codigo y no en una tabla de catalogo,
 * porque las transiciones entre ellos tambien estan en el codigo: un estado que
 * ninguna parte del sistema supiera interpretar dejaria el ticket en una
 * situacion muerta, invisible para los filtros. Al ser un enum, el compilador
 * garantiza que solo existen los tres valores contemplados.</p>
 *
 * <p>El nombre de la constante se guarda tal cual en la columna
 * {@code s_estado} del ticket. Cada valor lleva asociada una etiqueta legible,
 * que es la que se muestra en la interfaz y en los documentos generados:
 * separar ambas permite cambiar el texto visible sin tocar lo almacenado.</p>
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

    /** Indica si el ticket ya termino su ciclo y no admite mas transiciones. */
    public boolean esFinal() {
        return this == CERRADO;
    }

    /**
     * Convierte a enum el texto recibido. Normaliza mayusculas, espacios
     * sobrantes y el espacio interior, de modo que admite tanto
     * {@code "EN PROCESO"} como {@code "EN_PROCESO"}.
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
