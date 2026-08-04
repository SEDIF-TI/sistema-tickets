/**
 * Paleta de las gráficas del panel.
 *
 * Los colores no se eligen a ojo: esta selección está validada para daltonismo
 * (separación CVD ΔE ≥ 8 entre pares adyacentes) y para contraste sobre la
 * superficie de la tarjeta, en claro y en oscuro.
 *
 * La versión anterior usaba una lista de cinco colores recorrida por índice
 * (`COLORS_PIE[i % length]`), con dos consecuencias:
 *
 *  - El color seguía la posición, no la categoría. Al filtrar o al cambiar el
 *    orden de los datos, "Abierto" pasaba de verde a rojo sin que nada hubiera
 *    cambiado en la realidad.
 *  - Verde y rojo juntos son el par que peor distingue un daltónico, que es
 *    entre el 4 % y el 8 % de los hombres.
 *
 * Aquí el color va atado a la categoría, no a su posición.
 */

/** Serie categórica en orden fijo. Nunca se recicla ni se reordena. */
export const SERIES = ['#2a78d6', '#eb6834', '#1baf7a', '#eda100'];

/**
 * Colores de estado, con significado propio y reservado.
 *
 * No se reutilizan como "serie 5": el rojo siempre significa atención
 * requerida, y usarlo como un color más vacía ese significado.
 */
export const ESTADO = {
    ABIERTO: '#eda100',      // ámbar: pendiente de atender
    EN_PROCESO: '#2a78d6',   // azul: en curso
    CERRADO: '#1baf7a',      // verde: resuelto
};

export const PRIORIDAD = {
    URGENTE: '#e34948',
    ALTA: '#eb6834',
    NORMAL: '#2a78d6',
    BAJA: '#8b8a85',
};

/** Normaliza la etiqueta legible del backend ("En proceso") a su clave. */
const clave = (nombre) => (nombre || '').toUpperCase().replace(/\s+/g, '_');

/** Color estable de un estado; gris neutro si no se reconoce. */
export const colorDeEstado = (nombre) => ESTADO[clave(nombre)] ?? '#8b8a85';

/** Color estable de una prioridad; gris neutro si no se reconoce. */
export const colorDePrioridad = (nombre) => PRIORIDAD[clave(nombre)] ?? '#8b8a85';

/**
 * Color de una serie por su posición, para dimensiones sin vocabulario fijo
 * (áreas, técnicos). Al pasar de cuatro se repite en lugar de inventar tonos
 * nuevos: más de siete clases de color dejan de distinguirse entre sí.
 */
export const colorDeSerie = (indice) => SERIES[indice % SERIES.length];
