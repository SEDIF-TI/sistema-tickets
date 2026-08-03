package com.sedif.sistema_tickets.core.ticket;

/**
 * Entrada de un catalogo servido desde un enum.
 *
 * <p>{@code valor} es el nombre de la constante, que es lo que viaja de vuelta
 * al servidor y lo que la interfaz debe comparar; {@code etiqueta} es el texto
 * que se muestra. Separarlos evita que renombrar una etiqueta rompa la logica
 * de una pantalla.</p>
 */
public record CatalogoResponse(String valor, String etiqueta) {
}
