import { useState, useEffect, useContext, useCallback } from 'react';
import {
    Box, Typography, Chip, Tooltip, Dialog, DialogTitle, DialogContent,
    DialogActions, Button, TextField, MenuItem, Paper, Stack, Divider, Alert,
    useMediaQuery
} from '@mui/material';
import { useTheme } from '@mui/material/styles';
import VisibilityIcon from '@mui/icons-material/Visibility';
import DirectionsRunIcon from '@mui/icons-material/DirectionsRun';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutlined';
import AssignmentIcon from '@mui/icons-material/Assignment';

import { ticketService } from '../../services/ticketService';
import { useWebSocket } from '../../context/useWebSocket.js';
import { AuthContext } from '../../context/AuthContext.jsx';
import { useNotification } from '../../context/NotificationContext.jsx';
import { useNetworkStatus } from '../../hooks/useNetworkStatus.jsx';
import { useTablaPaginada } from '../../hooks/useTablaPaginada.jsx';
import { formatearFechaHora, tiempoRelativo, truncar, toUpper } from '../../util/formater';

import DynamicTable from '../../components/DynamicTable';
import AccionesTabla from '../../components/AccionesTabla';

/** Estados por los que puede filtrarse la bandeja. */
const OPCIONES_ESTATUS = [
    { valor: '', etiqueta: 'Todos los estados' },
    { valor: 'ABIERTO', etiqueta: 'Abierto' },
    { valor: 'EN PROCESO', etiqueta: 'En proceso' },
    { valor: 'CERRADO', etiqueta: 'Cerrado' },
];

/** Longitud mínima de la justificación, para que sirva como constancia. */
const MINIMO_JUSTIFICACION = 10;

const colorEstatus = (estatus) => {
    switch ((estatus || '').toUpperCase()) {
        case 'CERRADO':
        case 'RESUELTO': return 'default';
        case 'EN PROCESO': return 'info';
        case 'ABIERTO':
        case 'ASIGNADO': return 'warning';
        default: return 'primary';
    }
};

/** Un ticket cerrado ya no admite acciones del técnico. */
const estaCerrado = (estatus) => ['CERRADO', 'RESUELTO'].includes((estatus || '').toUpperCase());

/**
 * Bandeja de trabajo del técnico de soporte.
 *
 * Cambios respecto a la versión anterior:
 *  - Traía el histórico completo con `GET /v1/tickets` sin paginar y lo
 *    troceaba en memoria: la búsqueda solo miraba lo ya descargado. Ahora la
 *    paginación, el orden, la búsqueda y el filtro los resuelve el backend.
 *  - Las doce metas del plan de trabajo estaban escritas a mano en el JSX;
 *    ahora se piden al catálogo del backend.
 *  - Los errores se tragaban en `console.error` y el usuario no se enteraba de
 *    nada; el `alert()` nativo se sustituye por validación en el propio
 *    formulario y avisos por notificación.
 *  - Las acciones eran botones de texto de ancho fijo; ahora son iconos.
 */
export default function PanelSoporte() {
    const { stompClient, isConnected } = useWebSocket();
    const { user } = useContext(AuthContext);
    const { notificar, notificarError, notificarAdvertencia } = useNotification();

    const theme = useTheme();
    // Los diálogos pasan a pantalla completa en el teléfono: el técnico
    // resuelve el ticket de pie y en la calle, y un cuadro flotante con el
    // teclado abierto deja el campo de texto fuera de la vista.
    const esMovil = useMediaQuery(theme.breakpoints.down('sm'));

    const estadoRed = useNetworkStatus();
    const sinConexion = Boolean(estadoRed.sinConexion ?? estadoRed);

    const [filtroEstatus, setFiltroEstatus] = useState('');
    const [ticketDetalle, setTicketDetalle] = useState(null);
    const [ticketAResolver, setTicketAResolver] = useState(null);

    const [justificacion, setJustificacion] = useState('');
    const [planClave, setPlanClave] = useState('');
    const [intentoEnvio, setIntentoEnvio] = useState(false);
    const [resolviendo, setResolviendo] = useState(false);

    // Ticket cuyo aviso de "voy en camino" está viajando al servidor. Sin esto,
    // un segundo toque sobre el mismo botón —fácil desde un teléfono con la red
    // lenta— envía la petición dos veces.
    const [avisando, setAvisando] = useState(null);

    const [metas, setMetas] = useState([]);

    const cargar = useCallback((params) => ticketService.getAll(params), []);

    const tabla = useTablaPaginada({
        cargar,
        ordenInicial: { campo: 'fechaCreacion', direccion: 'desc' },
        filtros: { estatus: filtroEstatus || undefined },
    });

    // `recargar` es estable (useCallback sin dependencias en el hook), así que
    // se puede usar dentro de efectos sin reengancharlos en cada render.
    const { recargar } = tabla;

    // --- Catálogo del plan de trabajo -------------------------------------
    // Se pide una sola vez: es un catálogo fijo, no cambia entre resoluciones.
    useEffect(() => {
        let cancelado = false;

        ticketService.getPlanTrabajo()
            .then((respuesta) => {
                if (!cancelado) setMetas(respuesta?.data ?? []);
            })
            .catch(() => {
                // No se interrumpe la pantalla: la tabla sigue siendo usable y
                // el desplegable avisa por su cuenta de que está vacío.
                if (!cancelado) setMetas([]);
            });

        return () => { cancelado = true; };
    }, []);

    // --- Actualización en vivo --------------------------------------------
    // El servidor publica el ticket completo, pero la tabla está paginada y
    // ordenada en el backend: insertar la fila a mano dejaría la página
    // descuadrada respecto al total. Se recarga la página actual.
    useEffect(() => {
        if (!isConnected || !stompClient?.connected || !user) return;

        const miId = user.usuarioId ?? user.id;

        const suscripcion = stompClient.subscribe('/topic/tickets-soporte', (mensaje) => {
            try {
                const entrante = JSON.parse(mensaje.body);
                if (entrante.usuarioSoporteId === miId) recargar();
            } catch {
                // Un mensaje mal formado no debe tumbar la suscripción.
            }
        });

        return () => suscripcion?.unsubscribe();
    }, [isConnected, stompClient, user, recargar]);

    // --- Acciones ----------------------------------------------------------
    const marcarEnCamino = async (ticket) => {
        if (avisando) return;   // ya hay un aviso en curso

        setAvisando(ticket.id);
        try {
            const respuesta = await ticketService.atender(ticket.id);
            notificar(
                respuesta?.mensaje
                || `Avisaste que vas en camino al ticket #${ticket.id}.`
            );
            recargar();
        } catch (error) {
            notificarError(error);
        } finally {
            setAvisando(null);
        }
    };

    const abrirResolucion = (ticket) => {
        setTicketAResolver(ticket);
        setJustificacion('');
        setPlanClave('');
        setIntentoEnvio(false);
    };

    const cerrarResolucion = () => {
        if (resolviendo) return;   // no se cierra a media operación
        setTicketAResolver(null);
    };

    const justificacionInvalida = justificacion.trim().length < MINIMO_JUSTIFICACION;
    const planInvalido = !planClave;

    const confirmarResolucion = async () => {
        setIntentoEnvio(true);

        if (justificacionInvalida || planInvalido) {
            notificarAdvertencia('Revisa los campos marcados antes de confirmar.');
            return;
        }

        setResolviendo(true);
        try {
            const respuesta = await ticketService.resolver(
                ticketAResolver.id,
                justificacion.trim(),
                Number(planClave)
            );
            notificar(respuesta?.mensaje || `Ticket #${ticketAResolver.id} resuelto correctamente.`);
            setTicketAResolver(null);
            recargar();
        } catch (error) {
            notificarError(error);
        } finally {
            setResolviendo(false);
        }
    };

    // --- Columnas ----------------------------------------------------------
    const columnas = [
        {
            id: 'id',
            etiqueta: 'Folio',
            ancho: 90,
            ordenable: true,
            render: (t) => <Typography variant="body2" fontWeight={600}>#{t.id}</Typography>,
        },
        {
            id: 'titulo',
            etiqueta: 'Falla reportada',
            principal: true,
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
            etiqueta: 'Recibido',
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
                    variant={estaCerrado(t.estatus) ? 'outlined' : 'filled'}
                    sx={{ minWidth: 96 }}
                />
            ),
        },
        {
            id: 'acciones',
            etiqueta: 'Acciones',
            alineacion: 'center',
            ancho: 150,
            sinOrden: true,
            render: (t) => {
                const cerrado = estaCerrado(t.estatus);
                const enProceso = (t.estatus || '').toUpperCase() === 'EN PROCESO';
                const enviandoEste = avisando === t.id;

                return (
                    <AccionesTabla
                        acciones={[
                            {
                                id: 'ver',
                                icono: <VisibilityIcon />,
                                titulo: 'Ver detalle',
                                etiqueta: `Ver el detalle del ticket número ${t.id}`,
                                onClick: () => setTicketDetalle(t),
                            },
                            {
                                // Acción principal del técnico en campo: en el
                                // teléfono se muestra como botón con texto.
                                id: 'en-camino',
                                icono: <DirectionsRunIcon />,
                                titulo: enviandoEste ? 'Avisando…' : 'Voy en camino',
                                etiqueta: `Avisar que vas en camino al ticket número ${t.id}`,
                                color: 'warning',
                                prioritaria: true,
                                oculta: cerrado || enProceso,
                                onClick: () => marcarEnCamino(t),
                                deshabilitada: sinConexion || Boolean(avisando),
                                motivoDeshabilitada: sinConexion
                                    ? 'Sin conexión con el servidor'
                                    : 'Enviando el aviso…',
                            },
                            {
                                id: 'resolver',
                                icono: <CheckCircleOutlineIcon />,
                                titulo: 'Resolver ticket',
                                etiqueta: `Resolver el ticket número ${t.id}`,
                                color: 'success',
                                prioritaria: true,
                                oculta: cerrado || !enProceso,
                                onClick: () => abrirResolucion(t),
                                deshabilitada: sinConexion,
                                motivoDeshabilitada: 'Sin conexión con el servidor',
                            },
                        ]}
                    />
                );
            },
        },
    ];

    return (
        <Box>
            <Box sx={{ mb: 3 }}>
                <Typography
                    variant="h4"
                    component="h2"
                    color="primary.main"
                    sx={{ display: 'flex', alignItems: 'center', gap: 1 }}
                >
                    <AssignmentIcon fontSize="large" aria-hidden="true" />
                    Tickets asignados
                </Typography>
                <Typography variant="body2" color="text.secondary">
                    Reportes a tu cargo y los que tú mismo levantaste.
                </Typography>
            </Box>

            <DynamicTable
                columnas={columnas}
                filas={tabla.filas}
                cargando={tabla.cargando}
                anchoMinimo={1080}
                busqueda={tabla.busqueda}
                onBuscar={tabla.setBusqueda}
                placeholderBusqueda="Buscar por folio, falla, solicitante o área…"
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
                onRecargar={recargar}
                vacio={{
                    icono: AssignmentIcon,
                    titulo: tabla.busqueda || filtroEstatus
                        ? 'Sin resultados'
                        : 'No tienes tickets asignados',
                    descripcion: tabla.busqueda || filtroEstatus
                        ? 'Prueba con otros términos de búsqueda o cambia el filtro de estado.'
                        : 'Cuando se te asigne un reporte, aparecerá aquí.',
                }}
            />

            {/* ------------------------------------------------ detalle ---- */}
            <Dialog
                open={Boolean(ticketDetalle)}
                onClose={() => setTicketDetalle(null)}
                fullWidth
                maxWidth="sm"
                fullScreen={esMovil}
                aria-labelledby="titulo-detalle-ticket"
            >
                <DialogTitle id="titulo-detalle-ticket">
                    Detalle del ticket #{ticketDetalle?.id}
                </DialogTitle>

                <DialogContent dividers>
                    <Stack spacing={2.5}>
                        <Box>
                            <Typography variant="overline" color="text.secondary">
                                Falla reportada
                            </Typography>
                            <Typography variant="body1" fontWeight={500}>
                                {ticketDetalle?.titulo || '—'}
                            </Typography>
                        </Box>

                        <Box>
                            <Typography variant="overline" color="text.secondary">
                                Descripción
                            </Typography>
                            <Paper variant="outlined" sx={{ p: 2, mt: 0.5, bgcolor: 'grey.50' }}>
                                <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                                    {ticketDetalle?.descripcion || 'Sin descripción.'}
                                </Typography>
                            </Paper>
                        </Box>

                        <Divider />

                        {/* En móvil los datos se apilan en lugar de comprimirse. */}
                        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={{ xs: 2, sm: 4 }}>
                            <Box>
                                <Typography variant="overline" color="text.secondary">
                                    Solicitante
                                </Typography>
                                <Typography variant="body2">
                                    {ticketDetalle?.solicitante || '—'}
                                </Typography>
                            </Box>
                            <Box>
                                <Typography variant="overline" color="text.secondary">
                                    Área
                                </Typography>
                                <Typography variant="body2">
                                    {ticketDetalle?.departamento || '—'}
                                </Typography>
                            </Box>
                            <Box>
                                <Typography variant="overline" color="text.secondary">
                                    Sede
                                </Typography>
                                <Typography variant="body2">
                                    {ticketDetalle?.sede || '—'}
                                </Typography>
                            </Box>
                        </Stack>

                        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={{ xs: 2, sm: 4 }}>
                            <Box>
                                <Typography variant="overline" color="text.secondary">
                                    Recibido
                                </Typography>
                                <Typography variant="body2">
                                    {formatearFechaHora(ticketDetalle?.fechaCreacion)}
                                </Typography>
                            </Box>
                            <Box>
                                <Typography variant="overline" color="text.secondary">
                                    Cerrado
                                </Typography>
                                <Typography variant="body2">
                                    {formatearFechaHora(ticketDetalle?.fechaFin)}
                                </Typography>
                            </Box>
                        </Stack>

                        {ticketDetalle?.justificacion && (
                            <Box>
                                <Typography variant="overline" color="primary.main">
                                    Resolución registrada
                                </Typography>
                                <Paper
                                    variant="outlined"
                                    sx={{
                                        p: 2,
                                        mt: 0.5,
                                        borderLeft: '4px solid',
                                        borderLeftColor: 'primary.main',
                                    }}
                                >
                                    <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                                        {ticketDetalle.justificacion}
                                    </Typography>
                                </Paper>
                            </Box>
                        )}
                    </Stack>
                </DialogContent>

                <DialogActions sx={{ p: 2 }}>
                    <Button
                        onClick={() => setTicketDetalle(null)}
                        fullWidth={esMovil}
                        size={esMovil ? 'large' : 'medium'}
                    >
                        Cerrar
                    </Button>
                </DialogActions>
            </Dialog>

            {/* ---------------------------------------------- resolución ---- */}
            <Dialog
                open={Boolean(ticketAResolver)}
                onClose={cerrarResolucion}
                fullWidth
                maxWidth="sm"
                fullScreen={esMovil}
                aria-labelledby="titulo-resolver-ticket"
            >
                <DialogTitle id="titulo-resolver-ticket">
                    Resolver el ticket #{ticketAResolver?.id}
                </DialogTitle>

                <DialogContent dividers>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 2.5 }}>
                        El ticket quedará cerrado. Lo que escribas aquí es la constancia
                        del trabajo realizado y se guarda en la bitácora.
                    </Typography>

                    {metas.length === 0 && (
                        <Alert severity="warning" sx={{ mb: 2 }}>
                            No se pudo cargar el catálogo de metas del plan de trabajo.
                            Recarga la página antes de continuar.
                        </Alert>
                    )}

                    <Stack spacing={2.5}>
                        <TextField
                            select
                            required
                            fullWidth
                            label="Meta del plan de trabajo"
                            value={planClave}
                            onChange={(e) => setPlanClave(e.target.value)}
                            disabled={metas.length === 0 || resolviendo}
                            error={intentoEnvio && planInvalido}
                            helperText={
                                intentoEnvio && planInvalido
                                    ? 'Selecciona la meta a la que se imputa este trabajo.'
                                    : 'Determina en qué meta anual se contabiliza la atención.'
                            }
                        >
                            {metas.map((meta) => (
                                <MenuItem key={meta.clave} value={String(meta.clave)}>
                                    {meta.clave} — {meta.descripcion}
                                </MenuItem>
                            ))}
                        </TextField>

                        <TextField
                            required
                            fullWidth
                            multiline
                            rows={4}
                            label="Actividad de solución"
                            placeholder="EJ. SE REEMPLAZÓ EL CARTUCHO DE TÓNER NEGRO Y SE CALIBRÓ LA IMPRESORA."
                            value={justificacion}
                            onChange={(e) => setJustificacion(toUpper(e.target.value))}
                            disabled={resolviendo}
                            error={intentoEnvio && justificacionInvalida}
                            helperText={
                                intentoEnvio && justificacionInvalida
                                    ? `Describe el trabajo realizado (mínimo ${MINIMO_JUSTIFICACION} caracteres).`
                                    : 'Describe qué se hizo para resolver la falla.'
                            }
                        />
                    </Stack>
                </DialogContent>

                {/* En móvil los botones se apilan a lo ancho y en tamaño
                    grande: son el objetivo táctil final de todo el flujo. */}
                <DialogActions sx={{ flexDirection: { xs: 'column-reverse', sm: 'row' }, gap: 1, p: 2 }}>
                    <Button
                        onClick={cerrarResolucion}
                        color="inherit"
                        disabled={resolviendo}
                        fullWidth={esMovil}
                        size={esMovil ? 'large' : 'medium'}
                    >
                        Cancelar
                    </Button>
                    <Button
                        onClick={confirmarResolucion}
                        variant="contained"
                        disabled={resolviendo || sinConexion || metas.length === 0}
                        fullWidth={esMovil}
                        size={esMovil ? 'large' : 'medium'}
                    >
                        {resolviendo ? 'Guardando…' : 'Confirmar resolución'}
                    </Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
}
