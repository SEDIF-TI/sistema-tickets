package com.sedif.sistema_tickets.core.usuarios;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entrada del menu que el frontend dibuja para el usuario de la sesion.
 *
 * <p>Lleva solo lo que la barra lateral necesita: el texto, la ruta a la que
 * navega y el nombre del icono. El conjunto sale de cruzar el rol del usuario
 * con la tabla {@code rol_vista}.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VistaDTO {
    private String nombre;
    private String ruta;
    private String icono;
}