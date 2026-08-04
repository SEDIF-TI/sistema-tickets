package com.sedif.sistema_tickets.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * Traduce las excepciones de toda la aplicacion a respuestas
 * {@link ApiResponse} con el codigo HTTP adecuado.
 *
 * <p>Cumple dos objetivos:</p>
 * <ul>
 *   <li><b>Seguridad:</b> ninguna traza de pila ni detalle interno llega al
 *       cliente. Los fallos inesperados se registran completos en el servidor
 *       y hacia fuera se devuelve un mensaje generico.</li>
 *   <li><b>Consistencia:</b> los controladores dejan de necesitar
 *       {@code try/catch} y devuelven siempre el mismo formato.</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Errores de validacion de Bean Validation ({@code @Valid}).
     * Devuelve el detalle por campo para que el formulario los muestre.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> manejarValidacion(
            MethodArgumentNotValidException ex) {

        Map<String, String> errores = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.put(error.getField(), error.getDefaultMessage());
        }
        // Son errores del usuario, no del sistema: no ensucian los logs.
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(MessageConstants.DATOS_INVALIDOS, errores));
    }

    /** Reglas de negocio incumplidas. Los servicios lanzan esta excepcion. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> manejarArgumentoInvalido(IllegalArgumentException ex) {
        // El mensaje procede del propio codigo, no de la infraestructura,
        // por lo que es seguro mostrarlo.
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
    }

    /** Estado incompatible con la operacion solicitada. */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> manejarEstadoInvalido(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /** JSON mal formado o ilegible. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> manejarCuerpoIlegible(HttpMessageNotReadableException ex) {
        // No se propaga ex.getMessage(): revelaria nombres de clases internas.
        log.debug("Cuerpo de peticion ilegible: {}", ex.getMostSpecificCause().getClass().getSimpleName());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(MessageConstants.PETICION_MAL_FORMADA));
    }

    /** Falta un parametro obligatorio en la query string. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> manejarParametroFaltante(
            MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Falta el parametro obligatorio: " + ex.getParameterName()));
    }

    /** Tipo incorrecto en un parametro, por ejemplo texto donde se espera un id. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> manejarTipoInvalido(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("El valor de '" + ex.getName() + "' no tiene el formato esperado."));
    }

    /**
     * Usuario autenticado sin permisos suficientes.
     * Se responde 403, que es lo correcto cuando la identidad es conocida.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> manejarAccesoDenegado(AccessDeniedException ex) {
        log.warn("Acceso denegado a un recurso protegido.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(MessageConstants.ACCESO_DENEGADO));
    }

    /**
     * Regla de negocio que impide operar sobre un recurso ajeno.
     *
     * <p>Los servicios lanzan {@link SecurityException} cuando detectan, por
     * ejemplo, que alguien intenta cerrar un ticket de otra area. Sin este
     * manejador la excepcion caia en el {@code catch (Exception)} final y se
     * devolvia un <b>500</b>, haciendo pasar por error del servidor lo que en
     * realidad es una denegacion de permisos.</p>
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<Void>> manejarViolacionDePermisos(SecurityException ex) {
        log.warn("Intento de operar sobre un recurso fuera del alcance del usuario: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage() != null
                        ? ex.getMessage()
                        : MessageConstants.ACCESO_DENEGADO));
    }

    /**
     * Fallo de autenticacion: 401, no 403. La distincion importa porque
     * permite al frontend diferenciar "sesion caducada" de "sin permisos".
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> manejarAutenticacion(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(MessageConstants.AUTENTICACION_REQUERIDA));
    }

    /** Ruta inexistente. */
    /**
     * Ruta inexistente servida por el manejador de recursos estaticos.
     *
     * <p>Spring Boot no lanza {@link NoHandlerFoundException} para una URL sin
     * controlador: la pasa al manejador de recursos, que termina lanzando
     * {@code NoResourceFoundException}. Al no estar contemplada aqui, caia en
     * el manejador generico y <b>cualquier URL equivocada devolvia un 500</b>
     * en lugar de un 404, sugiriendo un fallo del servidor donde solo habia
     * una direccion mal escrita.</p>
     */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> manejarRecursoNoEncontrado(
            org.springframework.web.servlet.resource.NoResourceFoundException ex) {

        log.debug("Ruta no encontrada: {}", ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(MessageConstants.RECURSO_NO_ENCONTRADO));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> manejarRutaNoEncontrada(NoHandlerFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(MessageConstants.RECURSO_NO_ENCONTRADO));
    }

    /**
     * Red de seguridad para cualquier fallo no contemplado.
     *
     * <p>Se registra la excepcion completa en el servidor y al cliente solo se
     * le envia un mensaje generico. Es lo que impide que una traza de pila o
     * un error de base de datos acabe en el navegador.</p>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> manejarErrorInesperado(Exception ex) {
        log.error("Error no controlado en la aplicacion", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(MessageConstants.ERROR_INTERNO));
    }
}
