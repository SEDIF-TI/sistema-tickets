/**
 * Utilidades de formato compartidas.
 *
 * Cada pantalla tenía su propia función `formatearFecha`, con formatos
 * ligeramente distintos: la misma fecha se veía diferente según dónde
 * estuvieras. Aquí quedan centralizadas.
 */

/** Convierte a mayúsculas si es texto; el resto de valores pasan intactos. */
export const toUpper = (valor) => (typeof valor === 'string' ? valor.toUpperCase() : valor);

/**
 * Fecha y hora en formato corto: `05/02/26 14:30`.
 * Es el formato de las tablas, donde el espacio es escaso.
 */
export const formatearFechaHora = (fecha) => {
    if (!fecha) return '—';

    const d = new Date(fecha);
    if (Number.isNaN(d.getTime())) return '—';

    const dd = String(d.getDate()).padStart(2, '0');
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const aa = String(d.getFullYear()).slice(-2);
    const hh = String(d.getHours()).padStart(2, '0');
    const mi = String(d.getMinutes()).padStart(2, '0');

    return `${dd}/${mm}/${aa} ${hh}:${mi}`;
};

/** Solo la fecha: `05/02/2026`. Para documentos y vistas de detalle. */
export const formatearFecha = (fecha) => {
    if (!fecha) return '—';

    const d = new Date(fecha);
    if (Number.isNaN(d.getTime())) return '—';

    return d.toLocaleDateString('es-MX', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
    });
};

/** Fecha larga en español: `5 de febrero de 2026`. Para encabezados. */
export const formatearFechaLarga = (fecha) => {
    if (!fecha) return '—';

    const d = new Date(fecha);
    if (Number.isNaN(d.getTime())) return '—';

    return d.toLocaleDateString('es-MX', {
        day: 'numeric',
        month: 'long',
        year: 'numeric',
    });
};

/**
 * Tiempo transcurrido en lenguaje natural: "hace 3 días".
 * Permite ver de un vistazo la antigüedad de un ticket.
 */
export const tiempoRelativo = (fecha) => {
    if (!fecha) return '—';

    const d = new Date(fecha);
    if (Number.isNaN(d.getTime())) return '—';

    const segundos = Math.floor((Date.now() - d.getTime()) / 1000);

    if (segundos < 60) return 'hace unos segundos';
    if (segundos < 3600) {
        const min = Math.floor(segundos / 60);
        return `hace ${min} ${min === 1 ? 'minuto' : 'minutos'}`;
    }
    if (segundos < 86400) {
        const horas = Math.floor(segundos / 3600);
        return `hace ${horas} ${horas === 1 ? 'hora' : 'horas'}`;
    }

    const dias = Math.floor(segundos / 86400);
    if (dias < 30) return `hace ${dias} ${dias === 1 ? 'día' : 'días'}`;
    if (dias < 365) {
        const meses = Math.floor(dias / 30);
        return `hace ${meses} ${meses === 1 ? 'mes' : 'meses'}`;
    }

    const anios = Math.floor(dias / 365);
    return `hace ${anios} ${anios === 1 ? 'año' : 'años'}`;
};

/**
 * Días que faltan para una fecha. Negativo si ya pasó.
 * Lo usa el módulo de resguardos para avisar de vencimientos próximos.
 */
export const diasHasta = (fecha) => {
    if (!fecha) return null;

    const d = new Date(fecha);
    if (Number.isNaN(d.getTime())) return null;

    // Se comparan días completos, no horas: un resguardo que vence hoy a las
    // 23:00 no debe contar como vencido a las 09:00 de esa misma mañana.
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    const objetivo = new Date(d);
    objetivo.setHours(0, 0, 0, 0);

    return Math.round((objetivo - hoy) / 86400000);
};

/** Recorta un texto largo añadiendo puntos suspensivos. */
export const truncar = (texto, maximo = 60) => {
    if (!texto) return '';
    return texto.length > maximo ? `${texto.slice(0, maximo)}…` : texto;
};
