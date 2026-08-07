package com.sedif.sistema_tickets.core.documento;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Datos del formato de entrada de equipo.
 *
 * <p>Documenta el traslado de uno o varios equipos entre dos ubicaciones, con
 * los responsables de origen y destino y las firmas de entrega y recepcion.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntradaEquipoRequest {
    private String fecha;
    private Integer folioTicket;

    /** Un mismo formato puede amparar varios equipos, cada uno en su renglon. */
    private List<EntradaEquipoDetalleDTO> equipos;

    // Ubicacion de origen
    private String departamentoActual;
    private String direccionActual;
    private String telefonoActual;
    private String responsableActual;
    
    // Ubicacion de destino
    private String resguardoDestino;
    private String direccionDestino;
    private String telefonoDestino;
    private String responsableDestino;
    
    // Notas
    private String concepto;
    private String observacion;
    
    // Firmas
    private String entregadoPor;
    private String recibidoPor;
}