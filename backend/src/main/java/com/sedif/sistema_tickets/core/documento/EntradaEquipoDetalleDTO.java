package com.sedif.sistema_tickets.core.documento;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Renglon de la tabla de equipos del formato de entrada.
 *
 * <p>Los campos son texto libre porque se vuelcan tal cual en la celda del
 * PDF, incluida la cantidad.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntradaEquipoDetalleDTO {
    private String cve;
    private String descripcion;
    private String marca;
    private String modelo;
    private String serie;
    private String noResguardo;
    private String cantidad;
}