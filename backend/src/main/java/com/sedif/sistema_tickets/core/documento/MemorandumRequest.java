package com.sedif.sistema_tickets.core.documento;

/**
 * Record para recibir los datos del formulario de React
 * y armar el documento independiente.
 */
public record MemorandumRequest(
        String para,
        String de,
        String asunto,
        String cuerpo
) {}