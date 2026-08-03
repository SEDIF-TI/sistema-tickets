import { useState, useEffect, useCallback, useRef } from 'react';
import { useNotification } from '../context/NotificationContext.jsx';

/**
 * Estado de una tabla con paginación de servidor.
 *
 * Encapsula lo que, si no, cada pantalla repetiría: página, tamaño, orden,
 * búsqueda, carga de datos, control de errores y el retardo de la búsqueda.
 *
 * El backend devuelve `PageResponse`:
 *   { contenido, pagina, tamano, totalPaginas, totalItems, esPrimera, esUltima }
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

    // Texto tal como se teclea, y su versión retardada.
    const [busqueda, setBusqueda] = useState('');
    const [busquedaAplicada, setBusquedaAplicada] = useState('');

    // Contador para forzar recargas manuales sin duplicar dependencias.
    const [recargas, setRecargas] = useState(0);

    // Los filtros son un objeto nuevo en cada render; se guarda su forma
    // serializada para no disparar la carga en cada repintado.
    const filtrosSerializados = JSON.stringify(filtros);
    const filtrosRef = useRef(filtros);
    filtrosRef.current = filtros;

    // --- Retardo de la búsqueda -------------------------------------------
    // Sin esto se lanzaría una petición por cada tecla pulsada.
    useEffect(() => {
        const temporizador = setTimeout(() => {
            setBusquedaAplicada(busqueda);
            setPagina(0); // una búsqueda nueva empieza en la primera página
        }, 400);

        return () => clearTimeout(temporizador);
    }, [busqueda]);

    // --- Carga de datos ----------------------------------------------------
    useEffect(() => {
        if (!automatico && recargas === 0) return;

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

                // Si el componente se desmontó o llegó una petición posterior,
                // se descarta este resultado para no pisar datos más nuevos.
                if (cancelado) return;

                const datos = respuesta?.data;

                if (Array.isArray(datos?.contenido)) {
                    // Respuesta paginada del backend.
                    setFilas(datos.contenido);
                    setTotalItems(datos.totalItems ?? 0);
                } else if (Array.isArray(datos)) {
                    // Endpoint que aún devuelve una lista plana.
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
        // filtrosSerializados entra como dependencia en lugar del objeto para
        // comparar por valor y no por referencia.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [pagina, tamano, orden, busquedaAplicada, filtrosSerializados, recargas, automatico]);

    // --- Acciones ----------------------------------------------------------
    const cambiarPagina = useCallback((nueva) => setPagina(nueva), []);

    const cambiarTamano = useCallback((nuevo) => {
        setTamano(nuevo);
        setPagina(0);
    }, []);

    const cambiarOrden = useCallback((campo, direccion) => {
        setOrden({ campo, direccion });
        setPagina(0);
    }, []);

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
