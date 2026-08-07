package com.sedif.sistema_tickets.core.taller;

import java.util.List;

/**
 * Taller de reparaciones: ingreso de equipos, actualizacion del diagnostico y
 * avance por los estados del enum {@code EstadoTaller}.
 *
 * <p>Las operaciones de escritura reciben {@link EquipoReparacionRequest} y no
 * la entidad JPA, para que el cliente no pueda fijar campos que decide el
 * servidor (id, folio, tecnico asignado, columnas de auditoria).</p>
 *
 * <p>Hacia fuera se devuelven siempre {@link EquipoReparacionDTO}, nunca la
 * entidad: las relaciones perezosas se resuelven dentro de la transaccion.</p>
 */
public interface EquipoReparacionService {

    /** Volcado completo, sin paginar. Para el listado usar listarPaginado. */
    List<EquipoReparacionDTO> obtenerTodos();

    /** Listado paginado con busqueda y filtro de estado resueltos en la base. */
    com.sedif.sistema_tickets.exception.PageResponse<EquipoReparacionDTO> listarPaginado(
            String busqueda, String estado, org.springframework.data.domain.Pageable pageable);

    /** Busqueda por folio, solicitante, numero de serie o inventario. */
    List<EquipoReparacionDTO> buscarPorFiltro(String filtro);

    EquipoReparacionDTO obtenerPorId(Long id);

    /**
     * Registra la entrada de un equipo: genera el folio del ano en curso y lo
     * deja en estado RECIBIDO.
     *
     * @param tecnicoId tecnico que se hace cargo, opcional en el ingreso.
     */
    EquipoReparacionDTO registrarIngreso(EquipoReparacionRequest request, Long tecnicoId);

    /**
     * Actualiza los datos del equipo y el avance tecnico. No toca el folio ni
     * el estado, que tienen su propia via.
     */
    EquipoReparacionDTO actualizar(Long id, EquipoReparacionRequest request, Long tecnicoId);

    /** Avanza el estado del equipo, validando el valor contra el enum. */
    EquipoReparacionDTO cambiarEstado(Long id, String nuevoEstado);
}
