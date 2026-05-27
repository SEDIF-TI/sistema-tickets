package com.sedif.sistema_tickets.core.area;

public record AreaResponse(
        Long id,
        String nombre,
        Boolean activo,
        Long soporteFijoId,
        String soporteFijoNombre
) {
    /**
     * Transforma una Entidad Area en un Record de respuesta de forma segura.
     */
    public static AreaResponse desdeEntidad(Area area) {
        return new AreaResponse(
                area.getId(),
                area.getNombre(),
                area.getActivo(),
                area.getSoporteFijo() != null ? area.getSoporteFijo().getId() : null,
                area.getSoporteFijo() != null ? area.getSoporteFijo().getNombre() : "Sin soporte fijo asignado"
        );
    }
}