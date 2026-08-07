package com.sedif.sistema_tickets.util.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Prioridad de atencion de un ticket.
 *
 * <p>En la base de datos la columna {@code s_prioridad} es un texto libre con
 * NORMAL por defecto. Este enum acota los valores admisibles y anade a cada uno
 * una etiqueta legible para la interfaz y un peso numerico.</p>
 *
 * <p>El peso da un orden explicito que el nombre por si solo no proporciona:
 * es lo que permite ordenar la bandeja por urgencia y comparar prioridades en
 * las metricas del panel.</p>
 */
public enum Prioridad {

    BAJA("Baja", 1),
    NORMAL("Normal", 2),
    ALTA("Alta", 3),
    URGENTE("Urgente", 4);

    private final String etiqueta;

    /** Peso de ordenacion: a mayor numero, antes se atiende. */
    private final int peso;

    Prioridad(String etiqueta, int peso) {
        this.etiqueta = etiqueta;
        this.peso = peso;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public int getPeso() {
        return peso;
    }

    /**
     * Valor aplicado cuando el ticket no especifica prioridad. Coincide con el
     * predeterminado de la columna {@code s_prioridad}.
     */
    public static Prioridad porDefecto() {
        return NORMAL;
    }

    /**
     * Convierte el texto almacenado al enum, normalizando mayusculas y espacios
     * sobrantes. Un valor desconocido devuelve {@link Optional#empty()}, para
     * que no rompa la consulta de un listado completo.
     */
    public static Optional<Prioridad> desde(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        String normalizado = valor.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(p -> p.name().equals(normalizado))
                .findFirst();
    }
}
