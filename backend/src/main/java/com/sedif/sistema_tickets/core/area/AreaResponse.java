package com.sedif.sistema_tickets.core.area;

public record AreaResponse(
        Long id,
        String nombre,
        Boolean activo,
        Long soporteFijoId,
        String soporteFijoNombre,
        Boolean prioritaria
) {
    /**
     * Mapea el area resolviendo el soporte fijo.
     *
     * <p>Comprueba el nulo antes de leerlo porque la relacion es opcional y
     * perezosa: un area sin tecnico asignado se describe con una etiqueta en
     * lugar de dejar el campo vacio.</p>
     */
    public static AreaResponse desdeEntidad(Area area) {
        return new AreaResponse(
                area.getId(),
                area.getNombre(),
                area.getActivo(),
                area.getSoporteFijo() != null ? area.getSoporteFijo().getId() : null,
                area.getSoporteFijo() != null ? area.getSoporteFijo().getNombre() : "Sin soporte fijo asignado",
                area.getPrioritaria()
        );
    }
}