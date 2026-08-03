import { useState, useEffect, useContext, useCallback } from 'react';
import { Box, Button, Typography, Chip, Tooltip } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutlined';
import ConfirmationNumberIcon from '@mui/icons-material/ConfirmationNumber';
import { useNavigate, useLocation } from 'react-router-dom';

import { ticketService } from '../../services/ticketService';
import { AuthContext } from '../../context/AuthContext.jsx';
import { useNotification } from '../../context/NotificationContext.jsx';
import { useNetworkStatus } from '../../hooks/useNetworkStatus.jsx';
import { useTablaPaginada } from '../../hooks/useTablaPaginada.jsx';
import { formatearFechaHora, tiempoRelativo, truncar } from '../../util/formater';

import DynamicTable from '../../components/DynamicTable';
import AccionesTabla from '../../components/AccionesTabla';
import ConfirmationDialog from '../../components/ConfirmationDialog';

/** Estados posibles de un ticket, para el filtro. */
const OPCIONES_ESTATUS = [
    { valor: '', etiqueta: 'Todos los estados' },
    { valor: 'ABIERTO', etiqueta: 'Abierto' },
    { valor: 'EN PROCESO', etiqueta: 'En proceso' },
    { valor: 'PENDIENTE', etiqueta: 'Pendiente' },
    { valor: 'CERRADO', etiqueta: 'Cerrado' },
];

/** Color del distintivo según el estado. */
const colorEstatus = (estatus) => {
    switch ((estatus || '').toUpperCase()) {
        case 'CERRADO': return 'default';
        case 'EN PROCESO': return 'info';
        case 'PENDIENTE': return 'warning';
        case 'ABIERTO': return 'primary';
        default: return 'default';
    }
};

/**
 * Historial de tickets del usuario, o bitácora global si es administrador.
 *
 * Cambios respecto a la versión anterior:
 *  - La tabla se construía a mano, sin paginación: traía el historial entero
 *    en cada carga y lo pintaba de golpe.
 *  - Usaba `window.confirm()` y `alert()` nativos, bloqueantes y sin estilo.
 *  - La acción "Finalizar" era un botón de texto que ensanchaba la tabla.
 *  - El color guinda estaba escrito a mano.
 */
export default function TicketsPage() {
    const { user } = useContext(AuthContext);
    const { notificar, notificarError, notificarInfo } = useNotification();
    const navigate = useNavigate();
    const location = useLocation();

    const estadoRed = useNetworkStatus();
    const sinConexion = Boolean(estadoRed.sinConexion ?? estadoRed);

    const [filtroEstatus, setFiltroEstatus] = useState('');
    const [ticketAFinalizar, setTicketAFinalizar] = useState(null);
    const [finalizando, setFinalizando] = useState(false);

    const rolCrudo = user?.rol || user?.role || user?.rolNombre || '';
    const rol = rolCrudo.replace('ROLE_', '').toUpperCase();
    const esAdministrador = rol === 'ADMINISTRADOR';

    const cargar = useCallback((params) => ticketService.getMisTickets(params), []);

    const tabla = useTablaPaginada({
        cargar,
        ordenInicial: { campo: 'fechaCreacion', direccion: 'desc' },
        filtros: { estatus: filtroEstatus || undefined },
    });

    // Mensaje traído desde la pantalla de creación de ticket.
    useEffect(() => {
        if (location.state?.mensajeExito) {
            notificarInfo(location.state.mensajeExito);
            window.history.replaceState({}, document.title);
        }
    }, [location, notificarInfo]);

    const confirmarFinalizacion = async () => {
        setFinalizando(true);
        try {
            await ticketService.finalizar(ticketAFinalizar.id);
            notificar(`Ticket #${ticketAFinalizar.id} finalizado correctamente.`);
            setTicketAFinalizar(null);
            tabla.recargar();
        } catch (error) {
            notificarError(error);
        } finally {
            setFinalizando(false);
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
                    label={t.estatus}
                    size="small"
                    color={colorEstatus(t.estatus)}
                    variant={t.estatus === 'CERRADO' ? 'outlined' : 'filled'}
                    sx={{ minWidth: 96 }}
                />
            ),
        },
        {
            id: 'acciones',
            etiqueta: 'Acciones',
            alineacion: 'center',
            ancho: 110,
            sinOrden: true,
            render: (t) => {
                const cerrado = t.estatus === 'CERRADO';

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
            {/* Cabecera: en móvil el título y el botón se apilan. */}
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
                        opciones: OPCIONES_ESTATUS,
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
