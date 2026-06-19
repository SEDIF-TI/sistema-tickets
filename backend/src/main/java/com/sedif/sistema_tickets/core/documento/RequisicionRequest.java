package com.sedif.sistema_tickets.core.documento;

import java.util.List;

public record RequisicionRequest(
        String solicitante,
        String justificacion,
        List<ArticuloDTO> articulos
) {}