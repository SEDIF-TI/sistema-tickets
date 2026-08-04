/**
 * Vocabulario de la encuesta de satisfacción.
 *
 * Espeja el enum `Calificacion` del backend y el CHECK de la columna
 * `s_calificacion`.
 */

export const CALIFICACION = {
    MALO: 'MALO',
    REGULAR: 'REGULAR',
    BUENO: 'BUENO',
};

const normalizar = (valor) => (valor || '').toUpperCase().trim();

/** Texto legible, como respaldo si el backend no envía la etiqueta. */
export const etiquetaCalificacion = (valor) => {
    switch (normalizar(valor)) {
        case CALIFICACION.MALO: return 'Malo';
        case CALIFICACION.REGULAR: return 'Regular';
        case CALIFICACION.BUENO: return 'Bueno';
        default: return valor || '—';
    }
};

/**
 * Color del distintivo.
 *
 * Se usan los colores de estado —reservados para significar situación— porque
 * aquí eso es justo lo que comunican: un servicio mal calificado exige actuar.
 */
export const colorCalificacion = (valor) => {
    switch (normalizar(valor)) {
        case CALIFICACION.BUENO: return 'success';
        case CALIFICACION.REGULAR: return 'warning';
        case CALIFICACION.MALO: return 'error';
        default: return 'default';
    }
};

/** Color de la media, para la gráfica del panel. Escala de 1 a 3. */
export const colorPromedio = (promedio) => {
    if (promedio == null) return '#8b8a85';
    if (promedio >= 2.5) return '#1baf7a';   // aqua: buena valoración
    if (promedio >= 1.8) return '#eda100';   // ámbar: valoración intermedia
    return '#e34948';                        // rojo: exige atención
};
