package com.sedif.sistema_tickets.core.taller;

import java.util.List;

/**
 * Taller de reparaciones.
 *
 * <p>Las operaciones de escritura reciben {@link EquipoReparacionRequest} y no
 * la entidad JPA: aceptar la entidad permitia al cliente enviar campos que no
 * le corresponden (id, folio, tecnico asignado, columnas de auditoria) y
 * falsear registros.</p>
 */
public interface EquipoReparacionService {

    List<EquipoReparacionDTO> obtenerTodos();

    /** Listado paginado con busqueda y filtro de estado resueltos en la base. */
    com.sedif.sistema_tickets.exception.PageResponse<EquipoReparacionDTO> listarPaginado(
            String busqueda, String estado, org.springframework.data.domain.Pageable pageable);

    List<EquipoReparacionDTO> buscarPorFiltro(String filtro);

    EquipoReparacionDTO obtenerPorId(Long id);

    EquipoReparacionDTO registrarIngreso(EquipoReparacionRequest request, Long tecnicoId);

    EquipoReparacionDTO actualizar(Long id, EquipoReparacionRequest request, Long tecnicoId);

    EquipoReparacionDTO cambiarEstado(Long id, String nuevoEstado);
}
