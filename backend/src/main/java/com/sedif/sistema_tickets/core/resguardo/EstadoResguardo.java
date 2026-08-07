package com.sedif.sistema_tickets.core.resguardo;

/**
 * Situacion de un equipo prestado.
 *
 * <p>El alta siempre entra como {@code ENTREGADO}. Desde ahi solo hay dos
 * salidas: {@code DEVUELTO}, cuando el equipo regresa, o {@code VENCIDO}, que
 * marca la revision programada de vencimientos al pasar el plazo sin
 * devolucion. Un resguardo sin fecha de vencimiento nunca llega a
 * {@code VENCIDO}.</p>
 */
public enum EstadoResguardo {
    ENTREGADO,
    DEVUELTO,
    VENCIDO
}