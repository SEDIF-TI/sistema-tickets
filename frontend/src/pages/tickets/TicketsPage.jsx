import { useState, useEffect, useCallback } from 'react';
import { Box, Button, Typography, Chip, Tooltip } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import StarBorderIcon from '@mui/icons-material/StarBorder';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutlined';
import ConfirmationNumberIcon from '@mui/icons-material/ConfirmationNumber';
import { useNavigate, useLocation } from 'react-router-dom';

import { ticketService } from '../../services/ticketService';
import { useNotification } from '../../context/NotificationContext.jsx';
import { useNetworkStatus } from '../../hooks/useNetworkStatus.jsx';
import { useRol } from '../../hooks/useRol.jsx';
import { useTablaPaginada } from '../../hooks/useTablaPaginada.jsx';
import { formatearFechaHora, tiempoRelativo, truncar } from '../../util/formater';

import {
    OPCIONES_ESTADO_TICKET, colorEstado, etiquetaEstado, estaCerrado,
} from '../../util/estadoTicket';
import { colorCalificacion, etiquetaCalificacion } from '../../util/calificacion';

import DynamicTable from '../../components/DynamicTable';
import AccionesTabla from '../../components/AccionesTabla';
import ConfirmationDialog from '../../components/ConfirmationDialog';
import EncuestaDialog from '../../components/EncuestaDialog';


/**
 * Historial de tickets del usuario, o bitácora global si es administrador.
 *
 * `GET /v1/tickets/mis-tickets` devuelve lo que corresponde a cada rol: al
 * empleado los reportes de su área y al administrador todos los de la
 * institución. De ahí que una sola pantalla sirva a ambos casos, cambiando
 * únicamente los textos y las acciones disponibles.
 *
 * El solicitante cierra aquí el ciclo de su ticket: "Finalizar" lo marca como
 * cerrado —se confirma antes, porque es irreversible— y, con el ticket ya
 * cerrado y sus datos frescos, se le ofrece la encuesta de satisfacción. La
 * calificación solo puede darse una vez y solo sobre un ticket cerrado, así
 * que la acción desaparece en cuanto hay respuesta registrada.
 *
 * El administrador observa sin intervenir: no cierra tickets ajenos ni tiene
 * columna de opinión, que es del solicitante sobre su propio reporte.
 *
 * `sinCabecera` permite incrustar la tabla como pestaña del historial
 * unificado, donde el título ya lo pone la página contenedora.
 */
export default function TicketsPage({ sinCabecera = false }) {
    const { esAdministrador } = useRol();
    const { notificar, notificarError, notificarInfo } = useNotification();
    const navigate = useNavigate();
    const location = useLocation();

    const estadoRed = useNetworkStatus();
    const sinConexion = Boolean(estadoRed.sinConexion ?? estadoRed);

    const [filtroEstatus, setFiltroEstatus] = useState('');
    const [ticketAFinalizar, setTicketAFinalizar] = useState(null);
    const [finalizando, setFinalizando] = useState(false);

    // Ticket cuya encuesta se está mostrando. Se abre justo después de cerrar
    // el ticket, que es cuando el servicio está fresco y la respuesta vale.
    const [ticketAEncuestar, setTicketAEncuestar] = useState(null);
    const [enviandoEncuesta, setEnviandoEncuesta] = useState(false);

    const cargar = useCallback((params) => ticketService.getMisTickets(params), []);

    const tabla = useTablaPaginada({
        cargar,
        ordenInicial: { campo: 'fechaCreacion', direccion: 'desc' },
        filtros: { estatus: filtroEstatus || undefined },
    });

    // Confirmación del ticket recién levantado, que la pantalla de alta envía
    // por `state` al navegar. Se limpia para que no reaparezca al volver atrás.
    useEffect(() => {
        if (location.state?.mensajeExito) {
            notificarInfo(location.state.mensajeExito);
            window.history.replaceState({}, document.title);
        }
    }, [location, notificarInfo]);

    const confirmarFinalizacion = async () => {
        setFinalizando(true);
        try {
            const respuesta = await ticketService.finalizar(ticketAFinalizar.id);
            notificar(`Ticket #${ticketAFinalizar.id} finalizado correctamente.`);

            // La encuesta se ofrece con el ticket ya cerrado y sus datos
            // frescos, incluido el nombre del técnico que lo atendió.
            setTicketAEncuestar(respuesta?.data ?? ticketAFinalizar);
            setTicketAFinalizar(null);
            tabla.recargar();
        } catch (error) {
            notificarError(error);
        } finally {
            setFinalizando(false);
        }
    };

    const enviarEncuesta = async (calificacion, comentario) => {
        setEnviandoEncuesta(true);
        try {
            await ticketService.calificar(ticketAEncuestar.id, calificacion, comentario);
            notificar('Gracias por calificar el servicio.');
            setTicketAEncuestar(null);
            tabla.recargar();
        } catch (error) {
            notificarError(error);
        } finally {
            setEnviandoEncuesta(false);
        }
    };

    const columnas = [
        {
            id: 'id',
            etiqueta: 'Folio',
            ancho: 90,
            ordenable: true,
            render: (t) => (
                <Typography variant="body2" fontWeight={600}>#{t.id}</Typography>
            ),
        },
        {
            id: 'titulo',
            etiqueta: 'Asunto',
            principal: true,   // hace de título en la vista de tarjetas
            ordenable: true,
            render: (t) => (
                <Tooltip title={t.descripcion || ''} arrow placement="top-start">
                    <Typography variant="body2" fontWeight={500}>
                        {truncar(t.titulo, 48)}
                    </Typography>
                </Tooltip>
            ),
        },
        { id: 'solicitante', etiqueta: 'Solicitante', ancho: '15%' },
        { id: 'departamento', etiqueta: 'Área', ancho: '14%' },
        {
            id: 'usuarioSoporteNombre',
            etiqueta: 'Atiende',
            ancho: '14%',
            sinOrden: true,
            render: (t) => (
                t.usuarioSoporteNombre ? (
                    <Typography variant="body2">{t.usuarioSoporteNombre}</Typography>
                ) : (
                    <Tooltip title="Todavía no se ha asignado un técnico" arrow>
                        <Typography variant="body2" color="text.secondary">
                            Por asignar
                        </Typography>
                    </Tooltip>
                )
            ),
        },
        {
            id: 'fechaCreacion',
            etiqueta: 'Creado',
            ancho: 150,
            ordenable: true,
            sx: { whiteSpace: 'nowrap' },
            render: (t) => (
                <Tooltip title={tiempoRelativo(t.fechaCreacion)} arrow>
                    <Typography variant="body2" color="text.secondary">
                        {formatearFechaHora(t.fechaCreacion)}
                    </Typography>
                </Tooltip>
            ),
        },
        {
            id: 'fechaFin',
            etiqueta: 'Cerrado',
            ancho: 150,
            ordenable: true,
            sx: { whiteSpace: 'nowrap' },
            render: (t) => (
                <Typography variant="body2" color="text.secondary">
                    {formatearFechaHora(t.fechaFin)}
                </Typography>
            ),
        },
        {
            id: 'estatus',
            etiqueta: 'Estado',
            alineacion: 'center',
            ancho: 130,
            render: (t) => (
                <Chip
                    label={t.estatusEtiqueta || etiquetaEstado(t.estatus)}
                    size="small"
                    color={colorEstado(t.estatus)}
                    variant={estaCerrado(t.estatus) ? 'outlined' : 'filled'}
                    sx={{ minWidth: 96 }}
                />
            ),
        },
        {
            id: 'calificacion',
            etiqueta: 'Mi opinión',
            alineacion: 'center',
            ancho: 120,
            sinOrden: true,
            // Al administrador no le corresponde: es la bitácora global y esa
            // valoración la da cada solicitante sobre su propio ticket.
            oculta: esAdministrador,
            render: (t) => {
                if (t.calificacion) {
                    return (
                        <Tooltip title={t.comentarioEncuesta || 'Sin comentario'} arrow>
                            <Chip
                                label={t.calificacionEtiqueta || etiquetaCalificacion(t.calificacion)}
                                size="small"
                                color={colorCalificacion(t.calificacion)}
                                variant="outlined"
                            />
                        </Tooltip>
                    );
                }

                return (
                    <Typography variant="caption" color="text.secondary">
                        {estaCerrado(t.estatus) ? 'Sin calificar' : '—'}
                    </Typography>
                );
            },
        },
        {
            id: 'acciones',
            etiqueta: 'Acciones',
            alineacion: 'center',
            ancho: 110,
            sinOrden: true,
            render: (t) => {
                const cerrado = estaCerrado(t.estatus);

                // El administrador observa la bitácora; no cierra tickets ajenos.
                if (esAdministrador) {
                    return (
                        <Typography variant="caption" color="text.secondary">
                            {cerrado ? 'Finalizado' : 'En proceso'}
                        </Typography>
                    );
                }

                return (
                    <AccionesTabla
                        acciones={[
                            {
                                id: 'calificar',
                                icono: <StarBorderIcon />,
                                titulo: 'Calificar el servicio',
                                etiqueta: `Calificar el servicio del ticket número ${t.id}`,
                                color: 'primary',
                                // Solo tiene sentido en un ticket cerrado y sin
                                // calificar: quien ya respondió no puede
                                // cambiar su respuesta.
                                oculta: !cerrado || Boolean(t.calificacion),
                                onClick: () => setTicketAEncuestar(t),
                                deshabilitada: sinConexion,
                                motivoDeshabilitada: 'Sin conexión con el servidor',
                            },
                            {
                                id: 'finalizar',
                                icono: <CheckCircleOutlineIcon />,
                                titulo: 'Finalizar ticket',
                                etiqueta: `Finalizar el ticket número ${t.id}`,
                                color: 'success',
                                onClick: () => setTicketAFinalizar(t),
                                deshabilitada: cerrado || sinConexion,
                                motivoDeshabilitada: cerrado
                                    ? 'El ticket ya está cerrado'
                                    : 'Sin conexión con el servidor',
                            },
                        ]}
                    />
                );
            },
        },
    ];

    return (
        <Box>
            {/* En móvil el título y el botón se apilan. */}
            {!sinCabecera && (
            <Box
                sx={{
                    display: 'flex',
                    flexDirection: { xs: 'column', sm: 'row' },
                    justifyContent: 'space-between',
                    alignItems: { xs: 'stretch', sm: 'center' },
                    gap: 2,
                    mb: 3,
                }}
            >
                <Box>
                    <Typography variant="h4" component="h2" color="primary.main">
                        {esAdministrador ? 'Bitácora global de tickets' : 'Historial de tickets'}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        {esAdministrador
                            ? 'Todos los reportes registrados en la institución.'
                            : 'Reportes levantados por tu área.'}
                    </Typography>
                </Box>

                {!esAdministrador && (
                    <Button
                        variant="contained"
                        startIcon={<AddIcon />}
                        onClick={() => navigate('/tickets/nuevo')}
                        disabled={sinConexion}
                    >
                        Levantar ticket
                    </Button>
                )}
            </Box>
            )}

            <DynamicTable
                columnas={columnas}
                filas={tabla.filas}
                cargando={tabla.cargando}
                anchoMinimo={1080}
                busqueda={tabla.busqueda}
                onBuscar={tabla.setBusqueda}
                placeholderBusqueda="Buscar por asunto, solicitante o área…"
                filtros={[
                    {
                        id: 'estatus',
                        etiqueta: 'Estado',
                        valor: filtroEstatus,
                        valorPorDefecto: '',
                        opciones: OPCIONES_ESTADO_TICKET,
                        onChange: setFiltroEstatus,
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
                    icono: ConfirmationNumberIcon,
                    titulo: tabla.busqueda || filtroEstatus
                        ? 'Sin resultados'
                        : 'No hay tickets registrados',
                    descripcion: tabla.busqueda || filtroEstatus
                        ? 'Prueba con otros términos de búsqueda o cambia el filtro de estado.'
                        : esAdministrador
                            ? 'Cuando los usuarios levanten tickets, aparecerán aquí.'
                            : 'Aún no has levantado ningún ticket de soporte.',
                    textoAccion: !esAdministrador && !tabla.busqueda ? 'Levantar mi primer ticket' : undefined,
                    onAccion: !esAdministrador && !tabla.busqueda
                        ? () => navigate('/tickets/nuevo')
                        : undefined,
                }}
            />

            <EncuestaDialog
                abierto={Boolean(ticketAEncuestar)}
                ticket={ticketAEncuestar}
                enviando={enviandoEncuesta}
                onEnviar={enviarEncuesta}
                onOmitir={() => setTicketAEncuestar(null)}
            />

            <ConfirmationDialog
                abierto={Boolean(ticketAFinalizar)}
                titulo="¿Finalizar este ticket?"
                mensaje={
                    ticketAFinalizar
                        ? `El ticket #${ticketAFinalizar.id} "${ticketAFinalizar.titulo}" se marcará como cerrado. Confírmalo solo si tu problema quedó resuelto.`
                        : ''
                }
                textoConfirmar="Sí, finalizar"
                cargando={finalizando}
                onConfirmar={confirmarFinalizacion}
                onCancelar={() => setTicketAFinalizar(null)}
            />
        </Box>
    );
}
