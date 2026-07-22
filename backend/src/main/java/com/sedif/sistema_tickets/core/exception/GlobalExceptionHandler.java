package com.sedif.sistema_tickets.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralException(Exception ex) {
        Map<String, String> respuestaPersonalizada = new HashMap<>();
        
        // Estructura 100% controlada sin exponer el "path" ni datos sensibles
        respuestaPersonalizada.put("estado", "error");
        respuestaPersonalizada.put("mensaje", "Ocurrió un problema interno en el servidor al procesar la solicitud. Verifique los datos enviados.");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(respuestaPersonalizada);
    }
}