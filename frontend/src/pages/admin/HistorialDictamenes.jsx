import { useCallback } from 'react';
import { Box, Typography, Tooltip } from '@mui/material';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import FactCheckIcon from '@mui/icons-material/FactCheck';

import api from '../../services/api';
import { dictamenService } from '../../services/dictamenService';
import { useNotification } from '../../context/NotificationContext.jsx';
import { useNetworkStatus } from '../../hooks/useNetworkStatus.jsx';
import { useTablaPaginada } from '../../hooks/useTablaPaginada.jsx';
import { formatearFecha, truncar } from '../../util/formater';

import DynamicTable from '../../components/DynamicTable';
import AccionesTabla from '../../components/AccionesTabla';

/**
 * Historial de dictámenes técnicos: consulta y reimpresión.
 *
 * Hasta la migración V10 los dictámenes no se guardaban en ninguna parte: el
 * backend componía el PDF y lo devolvía, así que el único rastro era el archivo
 * que el técnico descargaba. Esta pantalla muestra los emitidos a partir de esa
 * migración; los anteriores no son recuperables porque nunca se escribieron.
 *
 * La emisión sigue estando en Documentos → Dictamen técnico. Aquí solo se
 * consulta y se vuelve a imprimir.
 */
export default function HistorialDictamenes() {
    const { notificar, notificarError } = useNotification();

    const estadoRed = useNetworkStatus();
    const sinConexion = Boolean(estadoRed.sinConexion ?? estadoRed);

    const cargar = useCallback((params) => dictamenService.getAll(params), []);

    const tabla = useTablaPaginada({
        cargar,
        ordenInicial: { campo: 'fechaCreacion', direccion: 'desc' },
    });

    const imprimir = async (dictamen) => {
        try {
            // Se reenvía al mismo endpoint que lo emitió. El backend registra un
            // dictamen por cada llamada, así que la reimpresión genera un folio
            // nuevo: es deliberado, cada documento firmado es una emisión.
            const respuesta = await api.post('/v1/documentos/dictamen', dictamen, {
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
            id: 'folio',
            etiqueta: 'Folio',
            ancho: 140,
            ordenable: true,
            sx: { whiteSpace: 'nowrap' },
            render: (d) => (
                <Typography variant="body2" fontWeight={600}>
                    {d.folio || '—'}
                </Typography>
            ),
        },
        {
            id: 'nombreUsuario',
            etiqueta: 'Servidor público',
            principal: true,
            ordenable: true,
            render: (d) => (
                <Box>
                    <Typography variant="body2" fontWeight={500}>
                        {truncar(d.nombreUsuario, 34) || '—'}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                        {d.departamentoUsuario || 'Sin departamento'}
                    </Typography>
                </Box>
            ),
        },
        {
            id: 'descripcionEquipo',
            etiqueta: 'Equipo',
            ancho: '22%',
            ordenable: true,
            render: (d) => (
                <Box>
                    <Typography variant="body2">
                        {truncar(d.descripcionEquipo, 30) || '—'}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                        {d.serie ? `Serie ${d.serie}` : 'Sin número de serie'}
                    </Typography>
                </Box>
            ),
        },
        {
            id: 'diagnostico',
            etiqueta: 'Diagnóstico',
            ancho: '24%',
            sinOrden: true,
            render: (d) => (
                <Tooltip title={d.diagnostico || ''} arrow>
                    <Typography variant="body2" color="text.secondary">
                        {truncar(d.diagnostico, 60) || '—'}
                    </Typography>
                </Tooltip>
            ),
        },
        {
            id: 'tecnicoNombre',
            etiqueta: 'Emitió',
            ancho: '16%',
            sinOrden: true,
            render: (d) => (
                <Typography variant="body2" color="text.secondary">
                    {truncar(d.tecnicoNombre || d.realizadoPor, 24) || '—'}
                </Typography>
            ),
        },
        {
            id: 'fechaCreacion',
            etiqueta: 'Emisión',
            ancho: 130,
            ordenable: true,
            sx: { whiteSpace: 'nowrap' },
            render: (d) => (
                <Typography variant="body2" color="text.secondary">
                    {formatearFecha(d.fechaCreacion)}
                </Typography>
            ),
        },
        {
            id: 'acciones',
            etiqueta: 'Acciones',
            alineacion: 'center',
            ancho: 100,
            sinOrden: true,
            render: (d) => (
                <AccionesTabla
                    acciones={[
                        {
                            id: 'pdf',
                            icono: <PictureAsPdfIcon />,
                            titulo: 'Reimprimir dictamen',
                            etiqueta: `Reimprimir el dictamen ${d.folio}`,
                            onClick: () => imprimir(d),
                            deshabilitada: sinConexion,
                            motivoDeshabilitada: 'Sin conexión con el servidor',
                        },
                    ]}
                />
            ),
        },
    ];

    return (
        <DynamicTable
            columnas={columnas}
            filas={tabla.filas}
            cargando={tabla.cargando}
            anchoMinimo={1140}
            busqueda={tabla.busqueda}
            onBuscar={tabla.setBusqueda}
            placeholderBusqueda="Buscar por folio, persona, equipo, marca o serie…"
            paginacion={tabla.paginacion}
            onCambiarPagina={tabla.cambiarPagina}
            onCambiarTamano={tabla.cambiarTamano}
            orden={tabla.orden}
            onCambiarOrden={tabla.cambiarOrden}
            onRecargar={tabla.recargar}
            vacio={{
                icono: FactCheckIcon,
                titulo: tabla.busqueda
                    ? 'Sin resultados'
                    : 'Todavía no hay dictámenes registrados',
                descripcion: tabla.busqueda
                    ? 'Prueba con otros términos de búsqueda.'
                    : 'Los dictámenes que se emitan desde Documentos aparecerán aquí. Los anteriores a esta versión no se guardaron.',
            }}
        />
    );
}
