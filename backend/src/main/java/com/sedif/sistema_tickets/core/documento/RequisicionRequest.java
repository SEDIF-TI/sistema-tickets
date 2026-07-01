package com.sedif.sistema_tickets.core.documento;

import java.util.List;

public record RequisicionRequest(
        String areaSolicitante,
        String fechaRequerida, // Agregamos la fecha que pusimos en React
        String justificacion,
        List<ArticuloDTO> listaArticulos // Mismo nombre que en React
) {}