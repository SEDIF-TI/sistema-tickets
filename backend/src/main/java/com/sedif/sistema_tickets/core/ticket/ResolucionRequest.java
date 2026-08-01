package com.sedif.sistema_tickets.core.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos con los que un tecnico cierra un ticket.
 *
 * <p>Sustituye al {@code Map<String, Object>} que recibia antes el endpoint de
 * resolucion: aceptaba cualquier estructura, obligaba a convertir los valores
 * a mano con {@code Integer.parseInt} (que lanza una excepcion no controlada
 * si el dato no es numerico) y no validaba nada.</p>
 *
 * <p>{@code planTrabajoClave} es opcional: no toda resolucion se imputa a una
 * meta del plan anual de trabajo.</p>
 */
public record ResolucionRequest(

        @NotBlank(message = "La justificacion de la resolucion es obligatoria.")
        @Size(max = 2000, message = "La justificacion no puede exceder 2000 caracteres.")
        String justificacion,

        Integer planTrabajoClave

) {}
