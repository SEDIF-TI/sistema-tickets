package com.sedif.sistema_tickets.core.ticket;

import java.time.LocalDateTime;

/**
 * Datos de un ticket tal como los consume el frontend.
 *
 * <p>Es un DTO: se envia solo lo necesario, sin exponer las entidades JPA ni
 * arrastrar sus relaciones perezosas.</p>
 *
 * <p><b>Sobre el estado.</b> Viajan dos campos a proposito:</p>
 * <ul>
 *   <li>{@code estatus} es el nombre de la constante del enum
 *       ({@code ABIERTO}, {@code EN_PROCESO}, {@code CERRADO}). Es el valor
 *       <b>estable</b>, el que la interfaz debe comparar para decidir que
 *       acciones muestra. Cambiar un texto visible nunca debe apagar un boton,
 *       que es justo lo que ocurriria si la logica dependiera de la etiqueta.</li>
 *   <li>{@code estatusEtiqueta} es el texto legible ("En proceso"), pensado
 *       solo para pintarse en pantalla y en los documentos.</li>
 * </ul>
 *
 * <p>El campo se llama {@code estatus} aunque el enum sea {@code EstadoTicket}:
 * es el nombre por el que lo leen las pantallas y forma parte del contrato de
 * la API.</p>
 */
public record TicketResponse(
    Long id,
    String titulo,
    String descripcion,
    String sede,
    LocalDateTime fechaCreacion,
    LocalDateTime fechaFin,
    String solicitante,
    String departamento,
    /** Constante del enum: valor estable para la logica de la interfaz. */
    String estatus,
    /** Texto legible del estado: solo para mostrar. */
    String estatusEtiqueta,
    /** Constante de la prioridad ({@code ALTA}, {@code URGENTE}...). */
    String prioridad,
    /** Texto legible de la prioridad: solo para mostrar. */
    String prioridadEtiqueta,
    Long usuarioAreaId,
    Long usuarioSoporteId,
    /**
     * Nombre del tecnico asignado, ya resuelto en el servidor.
     *
     * <p>Viaja resuelto y no solo por id porque quien levanta el ticket
     * necesita saber quien lo atiende, y un EMPLEADO no puede consultar el
     * listado de usuarios para traducir ese id por su cuenta.</p>
     */
    String usuarioSoporteNombre,
    String justificacion,
    /** Constante de la calificacion, o null si no se ha respondido. */
    String calificacion,
    /** Texto legible de la calificacion: solo para mostrar. */
    String calificacionEtiqueta,
    String comentarioEncuesta
) {}
