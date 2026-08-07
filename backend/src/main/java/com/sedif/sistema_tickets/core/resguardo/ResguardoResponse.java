package com.sedif.sistema_tickets.core.resguardo;

import java.time.LocalDateTime;

/**
 * Resguardo tal como lo consume el frontend.
 *
 * <p>Lleva todos los campos que el formato impreso necesita —incluidos
 * telefono, departamento, numero de inventario y condiciones de entrega—, de
 * modo que la pantalla puede mostrar y reimprimir la responsiva sin volver a
 * pedir nada.</p>
 *
 * <p>El estado viaja dos veces: {@code estado} es la constante del enum, valor
 * estable con el que la interfaz decide que acciones ofrece, y
 * {@code estadoEtiqueta} es el texto legible que se pinta.</p>
 */
public record ResguardoResponse(
        Long id,
        String solicitanteNombre,
        String solicitanteNumero,
        String telefono,
        String departamento,
        String equipoNombre,
        String numeroSerie,
        String numeroInventario,
        String accesorios,
        String condiciones,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaVencimiento,
        String estado,
        /** Texto legible del estado, para mostrarlo sin transformarlo. */
        String estadoEtiqueta,
        String creadoPor,
        Long usuarioCreadorId
) {
    public static ResguardoResponse desdeEntidad(Resguardo r) {
        return new ResguardoResponse(
                r.getId(),
                r.getSolicitanteNombre(),
                r.getSolicitanteNumero(),
                r.getTelefono(),
                r.getDepartamento(),
                r.getEquipoNombre(),
                r.getNumeroSerie(),
                r.getNumeroInventario(),
                r.getAccesorios(),
                r.getCondiciones(),
                r.getFechaCreacion(),
                r.getFechaVencimiento(),
                r.getEstado() != null ? r.getEstado().name() : null,
                etiqueta(r.getEstado()),
                r.getCreadoPor(),
                r.getUsuarioCreador() != null ? r.getUsuarioCreador().getId() : null
        );
    }

    private static String etiqueta(EstadoResguardo estado) {
        if (estado == null) {
            return null;
        }
        return switch (estado) {
            case ENTREGADO -> "Entregado";
            case DEVUELTO -> "Devuelto";
            case VENCIDO -> "Vencido";
        };
    }
}
