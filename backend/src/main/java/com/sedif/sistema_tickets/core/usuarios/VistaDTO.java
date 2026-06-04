package com.sedif.sistema_tickets.core.usuarios; // O el paquete de tus DTOs

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VistaDTO {
    private String nombre;
    private String ruta;
    private String icono;
}