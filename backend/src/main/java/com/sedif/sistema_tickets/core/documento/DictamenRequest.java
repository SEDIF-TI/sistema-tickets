package com.sedif.sistema_tickets.core.documento;

public record DictamenRequest(
        Long folioTicket,
        String fecha,
        String cve,
        String descripcionEquipo,
        String marca,
        String modelo,
        String serie,
        String noResguardo,
        String fallaReportada,
        String diagnostico,
        // El apartado de analisis tecnico se reduce a la falla y el
        // diagnostico: los campos de hallazgos y conclusion se retiraron del
        // formato oficial.
        String direccionUsuario,
        String departamentoUsuario,
        String nombreUsuario,
        String telefonoUsuario,
        String tipoReporte,
        // Firmas
        String realizadoPor,
        String revisadoPor,
        String recibidoPor
) {}