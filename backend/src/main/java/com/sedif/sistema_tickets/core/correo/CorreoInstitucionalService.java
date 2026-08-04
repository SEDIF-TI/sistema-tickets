package com.sedif.sistema_tickets.core.correo;

import java.util.List;

public interface CorreoInstitucionalService {

    List<CorreoInstitucional> obtenerTodos();

    List<CorreoInstitucional> buscarPorFiltro(String filtro);

    List<CorreoInstitucional> obtenerPorEstado(String estado);

    // Método necesario para combinar filtro de texto y filtro de estado (incluyendo BAJA)
    List<CorreoInstitucional> buscarPorFiltroYEstado(String filtro, String estado);

    /** Directorio paginado con busqueda y filtro de estado resueltos en la base. */
    com.sedif.sistema_tickets.exception.PageResponse<CorreoInstitucional> listarPaginado(
            String busqueda, String estado, org.springframework.data.domain.Pageable pageable);

    CorreoInstitucional obtenerPorId(Long id);

    CorreoInstitucional crear(CorreoRequest request);

    CorreoInstitucional actualizar(Long id, CorreoRequest request);

    CorreoInstitucional cambiarEstado(Long id, String nuevoEstado);

    void eliminar(Long id);
}