package com.sedif.sistema_tickets.core.ticket;

/**
 * Meta del plan anual de trabajo tal como la consume el frontend.
 *
 * <p>Se expone la clave numerica y su descripcion. La clave es la que se
 * guarda en {@code ticket.plan_trabajo_clave} y en {@code actividad_extra},
 * por lo que debe viajar tal cual, sin renumerar.</p>
 */
public record PlanTrabajoResponse(int clave, String descripcion) {
}
