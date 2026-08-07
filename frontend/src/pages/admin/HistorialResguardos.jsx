import { useState, useEffect, useCallback } from 'react';
import { Box, Typography, Chip, Tooltip } from '@mui/material';
import InventoryIcon from '@mui/icons-material/Inventory';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import { useLocation } from 'react-router-dom';

import api from '../../services/api';
import { resguardoService } from '../../services/resguardoService';
import { useNotification } from '../../context/NotificationContext.jsx';
import { useNetworkStatus } from '../../hooks/useNetworkStatus.jsx';
import { useTablaPaginada } from '../../hooks/useTablaPaginada.jsx';
import { formatearFecha, diasHasta, truncar } from '../../util/formater';
import {
    OPCIONES_ESTADO_RESGUARDO, colorEstadoResguardo, etiquetaEstadoResguardo, estaVigente,
} from '../../util/estadoResguardo';

import DynamicTable from '../../components/DynamicTable';
import AccionesTabla from '../../components/AccionesTabla';

/**
 * Historial de resguardos: consulta y reimpresión del documento.
 *
 * El alta de préstamos y el registro de devoluciones viven en
 * /soporte/resguardos; esta pantalla es solo de seguimiento. Los datos llegan
 * paginados desde el backend a través de useTablaPaginada, de modo que la
 * búsqueda y el filtro de estado se resuelven en la base y no sobre la página
 * ya descargada.
 *
 * La columna de vencimiento resalta en rojo los préstamos vigentes a los que
 * les quedan siete días o menos; los ya devueltos no se marcan aunque su fecha
 * haya pasado. Las alertas de vencimiento en pantalla las emite `MainLayout`
 * desde la conexión que mantiene `WebSocketContext`, así que aquí no se abre
 * ninguna suscripción propia.
 *
 * Admite la prop `sinCabecera` para montarse dentro de una pestaña del
 * historial unificado, donde el título lo pone la página contenedora.
 */
export default function HistorialResguardos({ sinCabecera = false }) {
    const location = useLocation();
    const { notificar, notificarError, notificarInfo } = useNotification();

    const estadoRed = useNetworkStatus();
    const sinConexion = Boolean(estadoRed.sinConexion ?? estadoRed);

    const [filtroEstado, setFiltroEstado] = useState('');

    const cargar = useCallback((params) => resguardoService.getAll(params), []);

    const tabla = useTablaPaginada({
        cargar,
        ordenInicial: { campo: 'fechaCreacion', direccion: 'desc' },
        filtros: { estado: filtroEstado || undefined },
    });

    // La pantalla de alta redirige aquí con el mensaje de éxito en el estado de
    // navegación. Se limpia con replaceState para que no reaparezca al recargar
    // o al volver atrás.
    useEffect(() => {
        if (location.state?.mensajeExito) {
            notificarInfo(location.state.mensajeExito);
            window.history.replaceState({}, document.title);
        }
    }, [location, notificarInfo]);

    // El PDF lo compone el backend a partir del resguardo enviado y vuelve como
    // blob, así que se envuelve en un object URL y se abre en otra pestaña. El
    // URL se revoca al minuto: antes de eso el visor del navegador todavía puede
    // estar leyéndolo.
    const imprimir = async (resguardo) => {
        try {
            const respuesta = await api.post('/v1/documentos/resguardos', resguardo, {
                responseType: 'blob',
            });

            const url = window.URL.createObjectURL(
                new Blob([respuesta.data], { type: 'application/pdf' })
            );
            const ventana = window.open(url, '_blank');

            if (!ventana) {
                notificarError('El navegador bloqueó la ventana. Permite las ventanas emergentes para ver el documento.');
            } else {
                notificar('Documento generado.');
            }

            setTimeout(() => window.URL.revokeObjectURL(url), 60000);
        } catch (error) {
            notificarError(error);
        }
    };

    const columnas = [
        {
            id: 'solicitanteNombre',
            etiqueta: 'Solicitante',
            principal: true,
            ordenable: true,
            render: (r) => (
                <Box>
                    <Typography variant="body2" fontWeight={500}>
                        {truncar(r.solicitanteNombre, 34)}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                        {r.departamento || 'Sin departamento'}
                    </Typography>
                </Box>
            ),
        },
        {
            id: 'equipoNombre',
            etiqueta: 'Equipo',
            ancho: '22%',
            ordenable: true,
            render: (r) => (
                <Box>
                    <Typography variant="body2">{truncar(r.equipoNombre, 30)}</Typography>
                    <Typography variant="caption" color="text.secondary">
                        {r.numeroSerie ? `Serie ${r.numeroSerie}` : 'Sin número de serie'}
                    </Typography>
                </Box>
            ),
        },
        {
            id: 'creadoPor',
            etiqueta: 'Registró',
            ancho: '16%',
            sinOrden: true,
            render: (r) => (
                <Typography variant="body2" color="text.secondary">
                    {truncar(r.creadoPor, 24) || '—'}
                </Typography>
            ),
        },
        {
            id: 'fechaCreacion',
            etiqueta: 'Entrega',
            ancho: 130,
            ordenable: true,
            sx: { whiteSpace: 'nowrap' },
            render: (r) => (
                <Typography variant="body2" color="text.secondary">
                    {formatearFecha(r.fechaCreacion)}
                </Typography>
            ),
        },
        {
            id: 'fechaVencimiento',
            etiqueta: 'Vence',
            ancho: 140,
            ordenable: true,
            sx: { whiteSpace: 'nowrap' },
            render: (r) => {
                if (!r.fechaVencimiento) {
                    return (
                        <Typography variant="body2" color="text.secondary">
                            Sin plazo
                        </Typography>
                    );
                }

                const dias = diasHasta(r.fechaVencimiento);
                const urgente = estaVigente(r.estado) && dias !== null && dias <= 7;

                return (
                    <Tooltip
                        title={!estaVigente(r.estado)
                            ? 'El equipo ya fue devuelto'
                            : dias < 0
                                ? `Venció hace ${Math.abs(dias)} día(s)`
                                : `Faltan ${dias} día(s)`}
                        arrow
                    >
                        <Typography
                            variant="body2"
                            color={urgente ? 'error.main' : 'text.secondary'}
                            fontWeight={urgente ? 600 : 400}
                        >
                            {formatearFecha(r.fechaVencimiento)}
                        </Typography>
                    </Tooltip>
                );
            },
        },
        {
            id: 'estado',
            etiqueta: 'Estado',
            alineacion: 'center',
            ancho: 130,
            render: (r) => (
                <Chip
                    label={r.estadoEtiqueta || etiquetaEstadoResguardo(r.estado)}
                    size="small"
                    color={colorEstadoResguardo(r.estado)}
                    variant={estaVigente(r.estado) ? 'filled' : 'outlined'}
                    sx={{ minWidth: 92 }}
                />
            ),
        },
        {
            id: 'acciones',
            etiqueta: 'Acciones',
            alineacion: 'center',
            ancho: 100,
            sinOrden: true,
            render: (r) => (
                <AccionesTabla
                    acciones={[
                        {
                            id: 'pdf',
                            icono: <PictureAsPdfIcon />,
                            titulo: 'Imprimir resguardo',
                            etiqueta: `Imprimir el resguardo de ${r.solicitanteNombre}`,
                            onClick: () => imprimir(r),
                            deshabilitada: sinConexion,
                            motivoDeshabilitada: 'Sin conexión con el servidor',
                        },
                    ]}
                />
            ),
        },
    ];

    return (
        <Box>
            {!sinCabecera && (
            <Box sx={{ mb: 3 }}>
                <Typography
                    variant="h4"
                    component="h2"
                    color="primary.main"
                    sx={{ display: 'flex', alignItems: 'center', gap: 1 }}
                >
                    <InventoryIcon fontSize="large" aria-hidden="true" />
                    Historial de resguardos
                </Typography>
                <Typography variant="body2" color="text.secondary">
                    Todos los préstamos registrados en la institución.
                </Typography>
            </Box>
            )}

            <DynamicTable
                columnas={columnas}
                filas={tabla.filas}
                cargando={tabla.cargando}
                anchoMinimo={1080}
                busqueda={tabla.busqueda}
                onBuscar={tabla.setBusqueda}
                placeholderBusqueda="Buscar por persona, equipo, serie o inventario…"
                filtros={[
                    {
                        id: 'estado',
                        etiqueta: 'Estado',
                        valor: filtroEstado,
                        valorPorDefecto: '',
                        opciones: OPCIONES_ESTADO_RESGUARDO,
                        onChange: setFiltroEstado,
                        ancho: 190,
                    },
                ]}
                paginacion={tabla.paginacion}
                onCambiarPagina={tabla.cambiarPagina}
                onCambiarTamano={tabla.cambiarTamano}
                orden={tabla.orden}
                onCambiarOrden={tabla.cambiarOrden}
                onRecargar={tabla.recargar}
                vacio={{
                    icono: InventoryIcon,
                    titulo: tabla.busqueda || filtroEstado
                        ? 'Sin resultados'
                        : 'No hay resguardos registrados',
                    descripcion: tabla.busqueda || filtroEstado
                        ? 'Prueba con otros términos o cambia el filtro de estado.'
                        : 'Cuando el área de soporte entregue equipo en préstamo, aparecerá aquí.',
                }}
            />
        </Box>
    );
}
