/**
 * Vocabulario de prioridades del ticket.
 *
 * Espeja el enum `Prioridad` del backend. Igual que con el estado, los valores
 * son los nombres de las constantes y no las etiquetas visibles: la lógica se
 * apoya en el código estable, y el texto solo se pinta.
 */

export const PRIORIDAD = {
    BAJA: 'BAJA',
    NORMAL: 'NORMAL',
    ALTA: 'ALTA',
    URGENTE: 'URGENTE',
};

/** Opciones del desplegable de filtro, con la entrada "todas" incluida. */
export const OPCIONES_PRIORIDAD = [
    { valor: '', etiqueta: 'Todas las prioridades' },
    { valor: PRIORIDAD.URGENTE, etiqueta: 'Urgente' },
    { valor: PRIORIDAD.ALTA, etiqueta: 'Alta' },
    { valor: PRIORIDAD.NORMAL, etiqueta: 'Normal' },
    { valor: PRIORIDAD.BAJA, etiqueta: 'Baja' },
];

const normalizar = (prioridad) => (prioridad || '').toUpperCase().trim();

/** Texto legible, como respaldo si el backend no envía `prioridadEtiqueta`. */
export const etiquetaPrioridad = (prioridad) => {
    switch (normalizar(prioridad)) {
        case PRIORIDAD.URGENTE: return 'Urgente';
        case PRIORIDAD.ALTA: return 'Alta';
        case PRIORIDAD.NORMAL: return 'Normal';
        case PRIORIDAD.BAJA: return 'Baja';
        default: return prioridad || '—';
    }
};

/**
 * Color del distintivo.
 *
 * Solo lo urgente y lo alto se destacan: si todo llama la atención, nada la
 * llama. Lo normal y lo bajo quedan en gris.
 */
export const colorPrioridad = (prioridad) => {
    switch (normalizar(prioridad)) {
        case PRIORIDAD.URGENTE: return 'error';
        case PRIORIDAD.ALTA: return 'warning';
        default: return 'default';
    }
};

/** Indica si la prioridad merece destacarse visualmente en la tabla. */
export const esDestacada = (prioridad) =>
    [PRIORIDAD.URGENTE, PRIORIDAD.ALTA].includes(normalizar(prioridad));
