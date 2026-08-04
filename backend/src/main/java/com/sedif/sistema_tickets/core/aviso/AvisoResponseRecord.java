package com.sedif.sistema_tickets.core.aviso;

/**
 * Aviso tal como lo consume el frontend.
 *
 * <p>Incluye {@code areaNombre} ya resuelto. Antes solo viajaba el
 * {@code areaId} y la pantalla lo cruzaba contra su propia lista de areas para
 * mostrar el nombre: si el area no estaba en esa lista —por estar dada de baja
 * o por caer fuera de la pagina cargada— el aviso aparecia como "Area
 * especifica", sin decir cual.</p>
 */
public record AvisoResponseRecord(
        Long id,
        String titulo,
        String mensaje,
        Boolean activo,
        Long areaId,
        String areaNombre
) {

    /** Mapeo sin area resuelta: el alcance se muestra como global. */
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
