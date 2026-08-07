package com.sedif.sistema_tickets.core.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos con los que un tecnico cierra un ticket.
 *
 * <p>Al ser un record tipado, la validacion ocurre antes de entrar al
 * controlador y {@code planTrabajoClave} llega ya convertido a {@code Integer}:
 * un valor no numerico se rechaza con un 400 en lugar de reventar dentro del
 * servicio.</p>
 *
 * <p>{@code planTrabajoClave} es opcional: no toda resolucion se imputa a una
 * meta del plan anual de trabajo.</p>
 */
public record ResolucionRequest(

        // El minimo de 10 caracteres evita cierres con "ok" o "listo", que no
        // sirven como constancia del trabajo realizado. El formulario del panel
        // de soporte comprueba lo mismo, pero solo para adelantar el aviso: la
        // regla que decide vive aqui.
        @NotBlank(message = "La justificacion de la resolucion es obligatoria.")
        @Size(min = 10, max = 2000,
              message = "Describe el trabajo realizado con al menos 10 caracteres.")
        String justificacion,

        Integer planTrabajoClave

) {}
