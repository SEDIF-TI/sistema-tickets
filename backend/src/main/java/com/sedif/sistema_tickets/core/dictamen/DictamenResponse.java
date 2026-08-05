package com.sedif.sistema_tickets.core.dictamen;

import java.time.LocalDateTime;

/**
 * Dictamen tal como lo consume el frontend.
 *
 * <p>Incluye todos los campos que el formato oficial imprime, para que la
 * reimpresion desde el historial no tenga que volver a pedir nada.</p>
 */
public record DictamenResponse(
        Long id,
        String folio,
        Long ticketId,
        String cve,
        String descripcionEquipo,
        String marca,
        String modelo,
        String serie,
        String noResguardo,
        String nombreUsuario,
        String telefonoUsuario,
        String direccionUsuario,
        String departamentoUsuario,
        String tipoReporte,
        String fallaReportada,
        String diagnostico,
        String realizadoPor,
        String revisadoPor,
        String recibidoPor,
        String tecnicoNombre,
        String creadoPor,
        LocalDateTime fechaCreacion
) {
    public static DictamenResponse desdeEntidad(Dictamen d) {
        return new DictamenResponse(
                d.getId(),
                d.getFolio(),
                d.getTicketId(),
                d.getCve(),
                d.getDescripcionEquipo(),
                d.getMarca(),
                d.getModelo(),
                d.getSerie(),
                d.getNoResguardo(),
                d.getNombreUsuario(),
                d.getTelefonoUsuario(),
                d.getDireccionUsuario(),
                d.getDepartamentoUsuario(),
                d.getTipoReporte(),
                d.getFallaReportada(),
                d.getDiagnostico(),
                d.getRealizadoPor(),
                d.getRevisadoPor(),
                d.getRecibidoPor(),
                d.getTecnico() != null ? d.getTecnico().getNombre() : null,
                d.getCreadoPor(),
                d.getFechaCreacion()
        );
    }
}
