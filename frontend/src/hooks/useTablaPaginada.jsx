import { useState, useEffect, useCallback, useRef } from 'react';
import { useNotification } from '../context/NotificationContext.jsx';

/**
 * Estado de una tabla con paginación de servidor.
 *
 * Centraliza página, tamaño, orden, búsqueda, carga de datos y aviso de errores,
 * y entrega esos valores con la forma que espera `DynamicTable`.
 *
 * Ciclo de carga: cualquier cambio en página, tamaño, orden, búsqueda aplicada,
 * filtros o contador de recargas lanza una llamada a `cargar` con
 * `{ page, size, sort, busqueda, ...filtros }`, donde `sort` viaja como
 * "campo,direccion". La respuesta se normaliza: si trae `PageResponse`
 * ({ contenido, pagina, tamano, totalPaginas, totalItems, esPrimera, esUltima })
 * se usan `contenido` y `totalItems`; si el endpoint todavía devuelve una lista
 * plana, esa lista pasa entera y el total es su longitud.
 *
 * La búsqueda se aplica con 400 ms de retardo para no lanzar una petición por
 * tecla pulsada, y cada carga marca las anteriores como canceladas: una
 * respuesta lenta que llegue tarde se descarta en lugar de pisar datos más
 * recientes o de escribir sobre un componente ya desmontado.
 *
 * Uso:
 *
 *   const tabla = useTablaPaginada({
 *     cargar: (params) => ticketService.getAll(params),
 *     ordenInicial: { campo: 'fechaCreacion', direccion: 'desc' },
 *   });
 *
 *   <DynamicTable
 *     filas={tabla.filas}
 *     cargando={tabla.cargando}
 *     paginacion={tabla.paginacion}
 *     onCambiarPagina={tabla.cambiarPagina}
 *     onCambiarTamano={tabla.cambiarTamano}
 *     orden={tabla.orden}
 *     onCambiarOrden={tabla.cambiarOrden}
 *     busqueda={tabla.busqueda}
 *     onBuscar={tabla.setBusqueda}
 *     onRecargar={tabla.recargar}
 *   />
 *
 * @param {Function} cargar        Recibe { page, size, sort, busqueda, ...filtros }.
 * @param {object}   ordenInicial  { campo, direccion } de partida.
 * @param {number}   tamanoInicial Filas por página (10 por defecto).
 * @param {object}   filtros       Filtros adicionales que se envían al backend.
 * @param {boolean}  automatico    Si es false, no carga hasta llamar a recargar().
 */
export function useTablaPaginada({
    cargar,
    ordenInicial = null,
    tamanoInicial = 10,
    filtros = {},
    automatico = true,
} = {}) {
    const { notificarError } = useNotification();

    const [filas, setFilas] = useState([]);
    const [cargando, setCargando] = useState(automatico);
    const [pagina, setPagina] = useState(0);
    const [tamano, setTamano] = useState(tamanoInicial);
    const [totalItems, setTotalItems] = useState(0);
    const [orden, setOrden] = useState(ordenInicial);

    // `busqueda` es lo que se teclea y alimenta el campo; `busquedaAplicada` es
    // lo que llega al backend, ya pasado el retardo.
    const [busqueda, setBusqueda] = useState('');
    const [busquedaAplicada, setBusquedaAplicada] = useState('');

    // Contador que sirve de disparador de las recargas manuales: incrementarlo
    // vuelve a ejecutar el efecto sin tocar ningún otro parámetro.
    const [recargas, setRecargas] = useState(0);

    // La pantalla suele declarar `filtros` en línea, así que es un objeto nuevo
    // en cada render. Se compara por su forma serializada para que un repintado
    // sin cambios reales no dispare otra petición; la referencia viva queda en
    // un ref para leerla siempre actualizada dentro del efecto.
    const filtrosSerializados = JSON.stringify(filtros);
    const filtrosRef = useRef(filtros);
    filtrosRef.current = filtros;

    // El temporizador se reinicia con cada pulsación, así que la búsqueda solo
    // se aplica cuando se deja de escribir durante 400 ms.
    useEffect(() => {
        const temporizador = setTimeout(() => {
            setBusquedaAplicada(busqueda);
            setPagina(0); // una búsqueda nueva empieza en la primera página
        }, 400);

        return () => clearTimeout(temporizador);
    }, [busqueda]);

    useEffect(() => {
        // En modo manual no se carga nada hasta la primera llamada a recargar().
        if (!automatico && recargas === 0) return;

        // Bandera de cancelación de esta ejecución concreta: la limpieza del
        // efecto la levanta, de modo que la petición en vuelo sabe que ya no es
        // la vigente cuando por fin responde.
        let cancelado = false;

        const obtener = async () => {
            setCargando(true);
            try {
                const params = {
                    page: pagina,
                    size: tamano,
                    ...filtrosRef.current,
                };

                if (orden?.campo) {
                    params.sort = `${orden.campo},${orden.direccion}`;
                }
                if (busquedaAplicada) {
                    params.busqueda = busquedaAplicada;
                }

                const respuesta = await cargar(params);

                // El componente se desmontó o ya salió una petición posterior:
                // este resultado se descarta para no pisar datos más nuevos.
                if (cancelado) return;

                const datos = respuesta?.data;

                if (Array.isArray(datos?.contenido)) {
                    // PageResponse: el total lo dicta el servidor, no el número
                    // de filas recibidas, que es solo el de esta página.
                    setFilas(datos.contenido);
                    setTotalItems(datos.totalItems ?? 0);
                } else if (Array.isArray(datos)) {
                    // Endpoint sin paginar: la lista completa cabe en una página.
                    setFilas(datos);
                    setTotalItems(datos.length);
                } else {
                    setFilas([]);
                    setTotalItems(0);
                }
            } catch (error) {
                if (cancelado) return;
                notificarError(error);
                setFilas([]);
                setTotalItems(0);
            } finally {
                if (!cancelado) setCargando(false);
            }
        };

        obtener();
        return () => { cancelado = true; };
        // La dependencia es `filtrosSerializados` y no el objeto `filtros`, para
        // compararlos por valor en lugar de por referencia.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [pagina, tamano, orden, busquedaAplicada, filtrosSerializados, recargas, automatico]);

    const cambiarPagina = useCallback((nueva) => setPagina(nueva), []);

    /** Cambia el tamaño y vuelve al inicio: la página actual puede no existir. */
    const cambiarTamano = useCallback((nuevo) => {
        setTamano(nuevo);
        setPagina(0);
    }, []);

    /** Reordena desde la primera página: el orden afecta al listado completo. */
    const cambiarOrden = useCallback((campo, direccion) => {
        setOrden({ campo, direccion });
        setPagina(0);
    }, []);

    /** Repite la carga con los parámetros actuales, sin moverse de página. */
    const recargar = useCallback(() => setRecargas((n) => n + 1), []);

    /** Vuelve al inicio manteniendo filtros: útil tras crear un registro. */
    const reiniciar = useCallback(() => {
        setPagina(0);
        setRecargas((n) => n + 1);
    }, []);

    return {
        filas,
        cargando,
        paginacion: { pagina, tamano, totalItems },
        orden,
        busqueda,
        setBusqueda,
        cambiarPagina,
        cambiarTamano,
        cambiarOrden,
        recargar,
        reiniciar,
        totalItems,
    };
}

export default useTablaPaginada;
