package com.sedif.sistema_tickets.core.aviso;

/**
 * Aviso tal como lo consume el frontend.
 *
 * <p>Incluye {@code areaNombre} ya resuelto en el servidor, de modo que la
 * pantalla no tenga que cruzar el {@code areaId} contra su propia lista de
 * areas: el nombre se muestra aunque el area este dada de baja o caiga fuera
 * de la pagina que el cliente tenga cargada.</p>
 */
public record AvisoResponseRecord(
        Long id,
        String titulo,
        String mensaje,
        Boolean activo,
        Long areaId,
        String areaNombre
) {

    /** Mapeo sin nombre de area: el alcance se muestra como global. */
    public static AvisoResponseRecord desdeEntidad(Aviso aviso) {
        return desdeEntidad(aviso, null);
    }

    public static AvisoResponseRecord desdeEntidad(Aviso aviso, String areaNombre) {
        return new AvisoResponseRecord(
                aviso.getId(),
                aviso.getTitulo(),
                aviso.getMensaje(),
                aviso.getActivo(),
                aviso.getAreaId(),
                areaNombre
        );
    }
}
