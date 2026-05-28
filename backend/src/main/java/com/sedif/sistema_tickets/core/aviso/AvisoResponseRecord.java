package com.sedif.sistema_tickets.core.aviso;

public record AvisoResponseRecord(
        Long id,
        String titulo,
        String mensaje,
        Boolean activo
) {
    public static AvisoResponseRecord desdeEntidad(Aviso aviso) {
        return new AvisoResponseRecord(
                aviso.getId(),
                aviso.getTitulo(),
                aviso.getMensaje(),
                aviso.getActivo()
        );
    }
}