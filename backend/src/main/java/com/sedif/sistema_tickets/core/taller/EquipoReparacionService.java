package com.sedif.sistema_tickets.core.taller;

import java.util.List;

public interface EquipoReparacionService {
    List<EquipoReparacionDTO> obtenerTodos();
    List<EquipoReparacionDTO> buscarPorFiltro(String filtro);
    EquipoReparacionDTO obtenerPorId(Long id);
    EquipoReparacionDTO registrarIngreso(EquipoReparacion equipo, Long tecnicoId);
    EquipoReparacionDTO actualizar(Long id, EquipoReparacion detalles, Long tecnicoId);
    EquipoReparacionDTO cambiarEstado(Long id, String nuevoEstado);
}