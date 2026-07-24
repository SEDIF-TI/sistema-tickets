package com.sedif.sistema_tickets.core.correo;

import java.util.List;

public interface CorreoInstitucionalService {

    List<CorreoInstitucional> obtenerTodos();

    List<CorreoInstitucional> buscarPorFiltro(String filtro);

    List<CorreoInstitucional> obtenerPorEstado(String estado);

    // Método necesario para combinar filtro de texto y filtro de estado (incluyendo BAJA)
    List<CorreoInstitucional> buscarPorFiltroYEstado(String filtro, String estado);

    CorreoInstitucional obtenerPorId(Long id);

    CorreoInstitucional crear(CorreoInstitucional correo);

    CorreoInstitucional actualizar(Long id, CorreoInstitucional correoActualizado);

    CorreoInstitucional cambiarEstado(Long id, String nuevoEstado);

    void eliminar(Long id);
}