package com.sedif.sistema_tickets.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Envoltorio uniforme de todas las respuestas de la API.
 *
 * <p>Formato: <code>{ success, message, data }</code>.</p>
 *
 * <p>Antes cada controlador devolvia una forma distinta: unas veces un texto
 * plano, otras la entidad JPA desnuda y otras un DTO. Eso obligaba al frontend
 * a tratar cada endpoint como un caso especial. Con este envoltorio el cliente
 * siempre puede leer {@code success} para saber si hubo error y {@code data}
 * para el contenido.</p>
 *
 * @param <T> tipo del contenido devuelto.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data
) {

    /** Respuesta correcta con datos y mensaje por defecto. */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, MessageConstants.OPERACION_EXITOSA, data);
    }

    /** Respuesta correcta con datos y mensaje propio. */
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, message, data);
    }

    /** Respuesta correcta sin contenido (por ejemplo, tras un borrado). */
    public static ApiResponse<Void> ok(String message) {
        return new ApiResponse<>(true, message, null);
    }

    /**
     * Respuesta de error. El mensaje debe ser apto para mostrarse al usuario:
     * nunca una traza de pila ni detalles internos del servidor.
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }

    /**
     * Respuesta de error con contenido adicional, como el mapa de campos que
     * fallaron una validacion.
     */
    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(false, message, data);
    }
}
