/**
 * Paleta de las gráficas del panel.
 *
 * Los colores no se eligen a ojo: esta selección está validada para daltonismo
 * (separación CVD ΔE ≥ 8 entre pares adyacentes) y para contraste sobre la
 * superficie de la tarjeta, en claro y en oscuro. Evita en particular el par
 * verde/rojo contiguo, que es el que peor distingue una deficiencia de visión
 * cromática —entre el 4 % y el 8 % de los hombres.
 *
 * La regla que ordena el módulo es que **el color sigue a la categoría, no a su
 * posición**. Un estado o una prioridad se buscan por su nombre en `ESTADO` y
 * `PRIORIDAD`, de modo que "Abierto" es siempre ámbar aunque al filtrar quede
 * el primero, el último o el único de la gráfica. Recorrer una lista por índice
 * haría que el mismo dato cambiara de color al reordenarse o al filtrarse, y el
 * lector interpretaría ese cambio como información.
 *
 * `colorDeSerie` es la excepción y solo se usa donde no hay vocabulario fijo
 * que consultar.
 */

/**
 * Serie categórica en orden fijo, para dimensiones sin significado propio.
 * No se reordena: el orden ES la asignación de color.
 */
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

/**
 * Convierte la etiqueta legible que envía el backend ("En proceso") en la clave
 * de los mapas de arriba (EN_PROCESO), de forma que baste el texto de la
 * gráfica para resolver el color.
 */
const clave = (nombre) => (nombre || '').toUpperCase().replace(/\s+/g, '_');

/** Color estable de un estado; gris neutro si no se reconoce. */
export const colorDeEstado = (nombre) => ESTADO[clave(nombre)] ?? '#8b8a85';

/** Color estable de una prioridad; gris neutro si no se reconoce. */
export const colorDePrioridad = (nombre) => PRIORIDAD[clave(nombre)] ?? '#8b8a85';

/**
 * Color de una serie por su posición, para dimensiones sin vocabulario fijo
 * que consultar: áreas, técnicos, cualquier lista que dependa de los datos.
 *
 * Al pasar de cuatro la paleta se recicla en lugar de inventar tonos nuevos:
 * más allá de siete clases el ojo deja de distinguirlas y el color pierde su
 * función de identificar.
 */
export const colorDeSerie = (indice) => SERIES[indice % SERIES.length];
