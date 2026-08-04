/**
 * Vocabulario de estados del resguardo.
 *
 * Espeja el enum `EstadoResguardo` del backend y el CHECK de la columna
 * `s_estado_resguardo`.
 */

export const ESTADO_RESGUARDO = {
    ENTREGADO: 'ENTREGADO',
    DEVUELTO: 'DEVUELTO',
    VENCIDO: 'VENCIDO',
};

export const OPCIONES_ESTADO_RESGUARDO = [
    { valor: '', etiqueta: 'Todos los estados' },
    { valor: ESTADO_RESGUARDO.ENTREGADO, etiqueta: 'Entregado' },
    { valor: ESTADO_RESGUARDO.VENCIDO, etiqueta: 'Vencido' },
    { valor: ESTADO_RESGUARDO.DEVUELTO, etiqueta: 'Devuelto' },
];

const normalizar = (estado) => (estado || '').toUpperCase().trim();

/** Texto legible, como respaldo si el backend no envía `estadoEtiqueta`. */
export const etiquetaEstadoResguardo = (estado) => {
    switch (normalizar(estado)) {
        case ESTADO_RESGUARDO.ENTREGADO: return 'Entregado';
        case ESTADO_RESGUARDO.DEVUELTO: return 'Devuelto';
        case ESTADO_RESGUARDO.VENCIDO: return 'Vencido';
        default: return estado || '—';
    }
};

/**
 * Color del distintivo.
 *
 * Vencido en rojo porque exige actuar; devuelto en gris porque el préstamo ya
 * se cerró y no requiere nada.
 */
export const colorEstadoResguardo = (estado) => {
    switch (normalizar(estado)) {
        case ESTADO_RESGUARDO.VENCIDO: return 'error';
        case ESTADO_RESGUARDO.ENTREGADO: return 'info';
        case ESTADO_RESGUARDO.DEVUELTO: return 'default';
        default: return 'default';
    }
};

/** El préstamo sigue abierto y el equipo continúa fuera. */
export const estaVigente = (estado) =>
    [ESTADO_RESGUARDO.ENTREGADO, ESTADO_RESGUARDO.VENCIDO].includes(normalizar(estado));
