package com.sedif.sistema_tickets.core.documento;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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