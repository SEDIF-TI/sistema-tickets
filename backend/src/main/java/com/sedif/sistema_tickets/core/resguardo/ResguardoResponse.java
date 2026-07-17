package com.sedif.sistema_tickets.core.resguardo;

import java.time.LocalDateTime;

public record ResguardoResponse(
        Long id,
        String solicitanteNombre,
        String solicitanteNumero,
        String equipoNombre,
        String numeroSerie,
        String accesorios,
        LocalDateTime fechaVencimiento,
        String estado,
        String creadoPor,
        Long usuarioCreadorId
) {
    public static ResguardoResponse desdeEntidad(Resguardo r) {
        return new ResguardoResponse(
                r.getId(),
                r.getSolicitanteNombre(),
                r.getSolicitanteNumero(),
                r.getEquipoNombre(),
                r.getNumeroSerie(),
                r.getAccesorios(),
                r.getFechaVencimiento(),
                r.getEstado().name(),
                r.getCreadoPor(),
                r.getUsuarioCreador().getId()
        );
    }
}