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
        String hallazgos,
        String conclusion,
        // Asegúrate de que estos 5 existan en tu record:
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