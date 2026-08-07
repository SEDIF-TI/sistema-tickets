package com.sedif.sistema_tickets.core.documento;

/**
 * Datos del dictamen tecnico que se emite al cerrar una reparacion.
 *
 * <p>Recoge lo que el formato oficial imprime: la identificacion del equipo,
 * los datos del usuario que reporto, el analisis tecnico y los tres nombres
 * que firman.</p>
 */
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
        // El apartado de analisis tecnico del formato oficial se compone de la
        // falla reportada y el diagnostico.
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