package com.sedif.sistema_tickets.util.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Calificacion que el solicitante da al servicio recibido.
 *
 * <p>Tres niveles y no cinco estrellas: la escala corta se contesta de un
 * vistazo y evita el punto medio ambiguo de las escalas pares. Quien acaba de
 * recibir una reparacion responde en un segundo, no rellena un cuestionario.</p>
 *
 * <p>El {@code peso} permite promediar: el panel muestra la media por tecnico,
 * y sin un valor numerico no habria forma de ordenarlos ni de comparar
 * periodos.</p>
 */
public enum Calificacion {

    MALO("Malo", 1),
    REGULAR("Regular", 2),
    BUENO("Bueno", 3);

    private final String etiqueta;

    /** Valor numerico para calcular promedios. */
    private final int peso;

    Calificacion(String etiqueta, int peso) {
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
     * Convierte el texto recibido, tolerando mayusculas y espacios.
     *
     * <p>Devuelve {@link Optional#empty()} si no corresponde a ningun valor
     * conocido, para que un dato historico invalido no rompa una consulta.</p>
     */
    public static Optional<Calificacion> desde(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        String normalizado = valor.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(c -> c.name().equals(normalizado))
                .findFirst();
    }
}
