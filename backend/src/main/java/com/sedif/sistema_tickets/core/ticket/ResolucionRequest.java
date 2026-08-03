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

        // El minimo de 10 caracteres evita cierres con "ok" o "listo", que no
        // sirven como constancia del trabajo realizado. Coincide con lo que
        // valida el formulario del panel de soporte: la regla vive aqui, y el
        // frontend solo la adelanta para no gastar un viaje al servidor.
        @NotBlank(message = "La justificacion de la resolucion es obligatoria.")
        @Size(min = 10, max = 2000,
              message = "Describe el trabajo realizado con al menos 10 caracteres.")
        String justificacion,

        Integer planTrabajoClave

) {}
