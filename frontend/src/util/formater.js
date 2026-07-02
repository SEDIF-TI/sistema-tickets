/**
 * Convierte los valores de un objeto a mayúsculas
 */
export const toUpper = (value) => {
    return typeof value === 'string' ? value.toUpperCase() : value;
};