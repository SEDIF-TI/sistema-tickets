/**
 * Vocabulario de estados del ticket.
 *
 * Espeja el enum `EstadoTicket` del backend, que sustituyó a la tabla catálogo
 * `estadoticket`. Los valores son los nombres de las constantes
 * (`EN_PROCESO`, con guion bajo), no las etiquetas visibles: el backend envía
 * ambos y la lógica debe apoyarse siempre en el código estable, para que
 * cambiar un texto de pantalla no apague un botón.
 *
 * Cada pantalla tenía antes su propia copia de esta lista, y ya diferían entre
 * sí: el panel de soporte contemplaba "RESUELTO" y "ASIGNADO", que el resto
 * desconocía.
 */

export const ESTADO_TICKET = {
    ABIERTO: 'ABIERTO',
    EN_PROCESO: 'EN_PROCESO',
    CERRADO: 'CERRADO',
};

/** Opciones del desplegable de filtro, con la entrada "todos" incluida. */
export const OPCIONES_ESTADO_TICKET = [
    { valor: '', etiqueta: 'Todos los estados' },
    { valor: ESTADO_TICKET.ABIERTO, etiqueta: 'Abierto' },
    { valor: ESTADO_TICKET.EN_PROCESO, etiqueta: 'En proceso' },
    { valor: ESTADO_TICKET.CERRADO, etiqueta: 'Cerrado' },
];

/**
 * Normaliza un estado recibido del servidor.
 *
 * Tolera la forma antigua con espacio ("EN PROCESO") por si algún endpoint sin
 * migrar todavía la envía, y los sinónimos históricos que el panel de soporte
 * trataba como estados propios.
 */
export const normalizarEstado = (estatus) => {
    const valor = (estatus || '').toUpperCase().replace(/\s+/g, '_');

    if (valor === 'RESUELTO') return ESTADO_TICKET.CERRADO;
    if (valor === 'ASIGNADO') return ESTADO_TICKET.EN_PROCESO;

    return valor;
};

/** Texto legible, como respaldo si el backend no envía `estatusEtiqueta`. */
export const etiquetaEstado = (estatus) => {
    switch (normalizarEstado(estatus)) {
        case ESTADO_TICKET.ABIERTO: return 'Abierto';
        case ESTADO_TICKET.EN_PROCESO: return 'En proceso';
        case ESTADO_TICKET.CERRADO: return 'Cerrado';
        default: return estatus || '—';
    }
};

/** Color del distintivo según el estado. */
export const colorEstado = (estatus) => {
    switch (normalizarEstado(estatus)) {
        case ESTADO_TICKET.CERRADO: return 'default';
        case ESTADO_TICKET.EN_PROCESO: return 'info';
        case ESTADO_TICKET.ABIERTO: return 'warning';
        default: return 'primary';
    }
};

/** El ticket ya terminó su ciclo y no admite más acciones del técnico. */
export const estaCerrado = (estatus) => normalizarEstado(estatus) === ESTADO_TICKET.CERRADO;

/** El técnico ya avisó que va en camino. */
export const estaEnProceso = (estatus) => normalizarEstado(estatus) === ESTADO_TICKET.EN_PROCESO;
