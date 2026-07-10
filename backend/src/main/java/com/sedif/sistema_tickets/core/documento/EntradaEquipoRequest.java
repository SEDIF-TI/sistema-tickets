package com.sedif.sistema_tickets.core.documento;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntradaEquipoRequest {
    private String fecha;
    private Integer folioTicket;
    
    // Lista de equipos
    private List<EntradaEquipoDetalleDTO> equipos;
    
    // Ubicación Actual
    private String departamentoActual;
    private String direccionActual;
    private String telefonoActual;
    private String responsableActual;
    
    // Ubicación Destino
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