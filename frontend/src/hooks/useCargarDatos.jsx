import { useState, useEffect, useCallback, useRef } from 'react';
import { useNotification } from '../context/NotificationContext.jsx';

/**
 * Carga de datos de un endpoint que no está paginado.
 *
 * Cubre el caso de los catálogos y los listados cortos —áreas, equipos de un
 * selector, avisos, métricas del panel—, donde no hay página ni orden que
 * negociar con el servidor y basta con traer el conjunto completo. Para las
 * tablas con paginación de servidor está `useTablaPaginada`.
 *
 * Uso:
 *
 *   const { datos, cargando, recargar } = useCargarDatos({
 *     cargar: () => areaService.getTodas(),
 *     valorInicial: [],
 *   });
 *
 * La petición se lanza al montar y cada vez que cambia `recargas`, que es lo
 * que incrementa `recargar()`. Cada carga marca las anteriores como canceladas,
 * de modo que una respuesta lenta que llegue tarde se descarta en lugar de
 * pisar datos más recientes o de escribir sobre un componente ya desmontado.
 *
 * `recargar(false)` repite la consulta sin volver a mostrar el estado de carga:
 * es lo que conviene tras guardar o borrar, donde el esqueleto en mitad de una
 * edición produce un parpadeo.
 *
 * @param {Function} cargar        Devuelve la promesa de la petición.
 * @param {*}        valorInicial  Valor mientras no hay datos (por defecto `[]`).
 * @param {Function} extraer       Obtiene los datos de la respuesta. Por defecto
 *                                 `respuesta.data`.
 * @param {boolean}  automatico    Si es `false`, no carga hasta llamar a `recargar()`.
 */
export function useCargarDatos({
    cargar,
    valorInicial = [],
    extraer,
    automatico = true,
} = {}) {
    const { notificarError } = useNotification();

    const [datos, setDatos] = useState(valorInicial);
    const [cargando, setCargando] = useState(automatico);
    const [recargas, setRecargas] = useState(0);
    const [conCarga, setConCarga] = useState(true);

    // El valor inicial y el extractor se guardan en refs para que redefinirlos
    // en línea, como hace cualquier pantalla, no dispare otra petición. La
    // asignación va dentro del efecto: escribir un ref durante el render deja
    // el valor a merced de los renders que React puede descartar.
    const valorInicialRef = useRef(valorInicial);
    const extraerRef = useRef(extraer);

    useEffect(() => {
        extraerRef.current = extraer;
    }, [extraer]);

    useEffect(() => {
        if (!automatico && recargas === 0) return;

        let cancelado = false;

        const obtener = async () => {
            if (conCarga) setCargando(true);
            try {
                const respuesta = await cargar();
                if (cancelado) return;

                const extractor = extraerRef.current;
                const contenido = extractor ? extractor(respuesta) : respuesta?.data;

                setDatos(contenido ?? valorInicialRef.current);
            } catch (error) {
                if (cancelado) return;
                notificarError(error);
                setDatos(valorInicialRef.current);
            } finally {
                if (!cancelado) setCargando(false);
            }
        };

        obtener();
        return () => { cancelado = true; };
    }, [cargar, recargas, automatico, conCarga, notificarError]);

    /**
     * Repite la consulta.
     *
     * @param {boolean} mostrarCarga Si es `false`, conserva los datos visibles
     *                               mientras la petición viaja.
     */
    const recargar = useCallback((mostrarCarga = true) => {
        setConCarga(mostrarCarga);
        setRecargas((n) => n + 1);
    }, []);

    return { datos, cargando, recargar, setDatos };
}

export default useCargarDatos;
