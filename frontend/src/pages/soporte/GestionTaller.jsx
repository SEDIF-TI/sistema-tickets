import { useState, useEffect, useCallback } from 'react';
import {
    Box, Typography, Button, Chip, Tooltip, Dialog, DialogTitle, DialogContent,
    DialogActions, Stack, useMediaQuery, Divider, TextField, MenuItem
} from '@mui/material';
import { useTheme } from '@mui/material/styles';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import HandymanIcon from '@mui/icons-material/Handyman';
import SwapHorizIcon from '@mui/icons-material/SwapHoriz';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import { tallerService } from '../../services/tallerService';
import { useNotification } from '../../context/NotificationContext.jsx';
import { useNetworkStatus } from '../../hooks/useNetworkStatus.jsx';
import { useTablaPaginada } from '../../hooks/useTablaPaginada.jsx';
import { esquemaTaller } from '../../util/esquemas';
import { formatearFecha, truncar } from '../../util/formater';

import DynamicTable from '../../components/DynamicTable';
import AccionesTabla from '../../components/AccionesTabla';
import CampoFormulario from '../../components/CampoFormulario';

/** Tipos de equipo más frecuentes, como sugerencia del desplegable. */
const TIPOS_EQUIPO = [
    { valor: 'PC DE ESCRITORIO', etiqueta: 'PC de escritorio' },
    { valor: 'LAPTOP', etiqueta: 'Laptop' },
    { valor: 'IMPRESORA', etiqueta: 'Impresora' },
    { valor: 'MONITOR', etiqueta: 'Monitor' },
    { valor: 'NO BREAK', etiqueta: 'No break' },
    { valor: 'ESCANER', etiqueta: 'Escáner' },
    { valor: 'OTRO', etiqueta: 'Otro' },
];

/** Estados terminales: el equipo ya salió del taller. */
const ESTADOS_FINALES = ['ENTREGADO', 'IRREPARABLE'];

const VALORES_INICIALES = {
    solicitanteNombre: '',
    solicitanteNumero: '',
    departamento: '',
    equipoTipo: 'PC DE ESCRITORIO',
    marca: '',
    modelo: '',
    numeroSerie: '',
    numeroInventario: '',
    condicionRecepcion: '',
    accesorios: '',
    fallaReportada: '',
    diagnostico: '',
    solucion: '',
};

/** Color del distintivo según el punto del ciclo de vida. */
const colorEstado = (estado) => {
    switch ((estado || '').toUpperCase()) {
        case 'RECIBIDO': return 'warning';
        case 'EN_DIAGNOSTICO':
        case 'EN_REPARACION': return 'info';
        case 'REPARADO': return 'success';
        case 'IRREPARABLE': return 'error';
        case 'ENTREGADO': return 'default';
        default: return 'default';
    }
};

/**
 * Taller: equipos recibidos para reparación.
 *
 * Cambios respecto a la versión anterior:
 *  - La lista de estados estaba escrita a mano con siete valores, y tres de
 *    ellos —ESPERA_REFACCIONES, DICTAMINADO y LISTO_PARA_ENTREGA— no existen
 *    en el enum `EstadoTaller`: seleccionarlos devolvía un error. Las métricas
 *    de la cabecera contaban precisamente esos estados inexistentes, así que
 *    "Equipos en taller" y "Listos para entrega" mostraban cifras erróneas.
 *  - `tallerService` envolvía los errores en `new Error(...)` leyendo un campo
 *    (`mensaje`) que el backend no envía, así que todos los fallos mostraban
 *    el texto genérico de respaldo en lugar del motivo real.
 *  - El listado no estaba paginado y la búsqueda exigía pulsar un botón.
 *  - El formulario no validaba nada antes de enviar.
 *  - Montaba su propio Snackbar en lugar de usar el del sistema.
 */
export default function GestionTaller() {
    const { notificar, notificarError } = useNotification();

    const theme = useTheme();
    const esMovil = useMediaQuery(theme.breakpoints.down('sm'));

    const estadoRed = useNetworkStatus();
    const sinConexion = Boolean(estadoRed.sinConexion ?? estadoRed);

    const [filtroEstado, setFiltroEstado] = useState('');
    const [estados, setEstados] = useState([]);

    const [equipoEditando, setEquipoEditando] = useState(null);
    const [modalAbierto, setModalAbierto] = useState(false);

    const [equipoCambioEstado, setEquipoCambioEstado] = useState(null);
    const [nuevoEstado, setNuevoEstado] = useState('');
    const [procesando, setProcesando] = useState(false);

    const cargar = useCallback((params) => tallerService.getAll(params), []);

    const tabla = useTablaPaginada({
        cargar,
        ordenInicial: { campo: 'fechaCreacion', direccion: 'desc' },
        filtros: { estado: filtroEstado || undefined },
    });

    const { recargar } = tabla;

    const {
        control,
        handleSubmit,
        reset,
        formState: { isSubmitting },
    } = useForm({
        resolver: zodResolver(esquemaTaller),
        mode: 'onBlur',
        defaultValues: VALORES_INICIALES,
    });

    // --- Catálogo de estados ----------------------------------------------
    useEffect(() => {
        let cancelado = false;

        tallerService.getEstados()
            .then((respuesta) => {
                if (!cancelado) setEstados(respuesta?.data ?? []);
            })
            .catch(() => {
                if (!cancelado) setEstados([]);
            });

        return () => { cancelado = true; };
    }, []);

    const abrirAlta = () => {
        setEquipoEditando(null);
        reset(VALORES_INICIALES);
        setModalAbierto(true);
    };

    const abrirEdicion = (equipo) => {
        setEquipoEditando(equipo);
        reset({
            solicitanteNombre: equipo.solicitanteNombre || '',
            solicitanteNumero: equipo.solicitanteNumero || '',
            departamento: equipo.departamento || '',
            equipoTipo: equipo.equipoTipo || 'PC DE ESCRITORIO',
            marca: equipo.marca || '',
            modelo: equipo.modelo || '',
            numeroSerie: equipo.numeroSerie || '',
            numeroInventario: equipo.numeroInventario || '',
            condicionRecepcion: equipo.condicionRecepcion || '',
            accesorios: equipo.accesorios || '',
            fallaReportada: equipo.fallaReportada || '',
            diagnostico: equipo.diagnostico || '',
            solucion: equipo.solucion || '',
        });
        setModalAbierto(true);
    };

    const guardar = async (datos) => {
        // Los campos vacíos viajan como null y no como cadena vacía: el
        // backend distingue "sin dato" de "dato en blanco".
        const carga = Object.fromEntries(
            Object.entries(datos).map(([clave, valor]) => [
                clave,
                typeof valor === 'string' ? (valor.trim() || null) : valor,
            ])
        );

        try {
            const respuesta = equipoEditando
                ? await tallerService.update(equipoEditando.id, carga)
                : await tallerService.create(carga);

            notificar(respuesta?.mensaje
                || (equipoEditando ? 'Registro actualizado.' : 'Equipo registrado en el taller.'));
            setModalAbierto(false);
            recargar();
        } catch (error) {
            notificarError(error);
        }
    };

    const abrirCambioEstado = (equipo) => {
        setEquipoCambioEstado(equipo);
        setNuevoEstado(equipo.estadoTaller || '');
    };

    const confirmarCambioEstado = async () => {
        setProcesando(true);
        try {
            const respuesta = await tallerService.cambiarEstado(equipoCambioEstado.id, nuevoEstado);
            notificar(respuesta?.mensaje || 'Estado actualizado.');
            setEquipoCambioEstado(null);
            recargar();
        } catch (error) {
            notificarError(error);
        } finally {
            setProcesando(false);
        }
    };

    const etiquetaDe = (valor) =>
        estados.find((e) => e.valor === valor)?.etiqueta || valor || '—';

    // --- Columnas ---------------------------------------------------------
    const columnas = [
        {
            id: 'folio',
            etiqueta: 'Folio',
            ancho: 120,
            ordenable: true,
            render: (e) => (
                <Typography variant="body2" fontWeight={600}>
                    {e.folio}
                </Typography>
            ),
        },
        {
            id: 'equipoTipo',
            etiqueta: 'Equipo',
            principal: true,
            ordenable: true,
            render: (e) => (
                <Box>
                    <Typography variant="body2" fontWeight={500}>
                        {[e.equipoTipo, e.marca].filter(Boolean).join(' · ')}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                        {e.numeroSerie ? `Serie ${e.numeroSerie}` : 'Sin número de serie'}
                    </Typography>
                </Box>
            ),
        },
        {
            id: 'solicitanteNombre',
            etiqueta: 'Solicitante',
            ancho: '18%',
            ordenable: true,
            render: (e) => (
                <Box>
                    <Typography variant="body2">{truncar(e.solicitanteNombre, 26)}</Typography>
                    <Typography variant="caption" color="text.secondary">
                        {e.departamento || 'Sin departamento'}
                    </Typography>
                </Box>
            ),
        },
        {
            id: 'fallaReportada',
            etiqueta: 'Falla',
            ancho: '20%',
            sinOrden: true,
            render: (e) => (
                <Tooltip title={e.fallaReportada || ''} arrow placement="top-start">
                    <Typography variant="body2" color="text.secondary">
                        {truncar(e.fallaReportada, 40)}
                    </Typography>
                </Tooltip>
            ),
        },
        {
            id: 'fechaCreacion',
            etiqueta: 'Ingreso',
            ancho: 130,
            ordenable: true,
            sx: { whiteSpace: 'nowrap' },
            render: (e) => (
                <Typography variant="body2" color="text.secondary">
                    {formatearFecha(e.fechaCreacion)}
                </Typography>
            ),
        },
        {
            id: 'estadoTaller',
            etiqueta: 'Estado',
            alineacion: 'center',
            ancho: 150,
            render: (e) => (
                <Chip
                    label={etiquetaDe(e.estadoTaller)}
                    size="small"
                    color={colorEstado(e.estadoTaller)}
                    variant={ESTADOS_FINALES.includes(e.estadoTaller) ? 'outlined' : 'filled'}
                    sx={{ minWidth: 110 }}
                />
            ),
        },
        {
            id: 'acciones',
            etiqueta: 'Acciones',
            alineacion: 'center',
            ancho: 120,
            sinOrden: true,
            render: (e) => {
                const cerrado = ESTADOS_FINALES.includes(e.estadoTaller);

                return (
                    <AccionesTabla
                        acciones={[
                            {
                                id: 'editar',
                                icono: <EditIcon />,
                                titulo: 'Ver y editar',
                                etiqueta: `Ver y editar el registro ${e.folio}`,
                                onClick: () => abrirEdicion(e),
                                deshabilitada: sinConexion,
                                motivoDeshabilitada: 'Sin conexión con el servidor',
                            },
                            {
                                id: 'estado',
                                icono: <SwapHorizIcon />,
                                titulo: 'Cambiar estado',
                                etiqueta: `Cambiar el estado del registro ${e.folio}`,
                                color: 'primary',
                                onClick: () => abrirCambioEstado(e),
                                deshabilitada: sinConexion || cerrado,
                                motivoDeshabilitada: cerrado
                                    ? 'El equipo ya salió del taller'
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
                    <Typography
                        variant="h4"
                        component="h2"
                        color="primary.main"
                        sx={{ display: 'flex', alignItems: 'center', gap: 1 }}
                    >
                        <HandymanIcon fontSize="large" aria-hidden="true" />
                        Taller
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        Equipos recibidos para diagnóstico y reparación.
                    </Typography>
                </Box>

                <Button
                    variant="contained"
                    startIcon={<AddIcon />}
                    onClick={abrirAlta}
                    disabled={sinConexion}
                >
                    Recibir equipo
                </Button>
            </Box>

            <DynamicTable
                columnas={columnas}
                filas={tabla.filas}
                cargando={tabla.cargando}
                anchoMinimo={1180}
                busqueda={tabla.busqueda}
                onBuscar={tabla.setBusqueda}
                placeholderBusqueda="Buscar por folio, equipo, serie o solicitante…"
                filtros={[
                    {
                        id: 'estado',
                        etiqueta: 'Estado',
                        valor: filtroEstado,
                        valorPorDefecto: '',
                        opciones: [
                            { valor: '', etiqueta: 'Todos los estados' },
                            ...estados.map((e) => ({ valor: e.valor, etiqueta: e.etiqueta })),
                        ],
                        onChange: setFiltroEstado,
                        ancho: 200,
                    },
                ]}
                paginacion={tabla.paginacion}
                onCambiarPagina={tabla.cambiarPagina}
                onCambiarTamano={tabla.cambiarTamano}
                orden={tabla.orden}
                onCambiarOrden={tabla.cambiarOrden}
                onRecargar={recargar}
                vacio={{
                    icono: HandymanIcon,
                    titulo: tabla.busqueda || filtroEstado
                        ? 'Sin resultados'
                        : 'No hay equipos en el taller',
                    descripcion: tabla.busqueda || filtroEstado
                        ? 'Prueba con otros términos o cambia el filtro de estado.'
                        : 'Registra aquí los equipos que se reciben para reparación.',
                    textoAccion: !tabla.busqueda && !filtroEstado ? 'Recibir el primer equipo' : undefined,
                    onAccion: !tabla.busqueda && !filtroEstado ? abrirAlta : undefined,
                }}
            />

            {/* --------------------------------------- alta y edición ------ */}
            <Dialog
                open={modalAbierto}
                onClose={() => !isSubmitting && setModalAbierto(false)}
                fullWidth
                maxWidth="md"
                fullScreen={esMovil}
                aria-labelledby="titulo-taller"
            >
                <DialogTitle id="titulo-taller">
                    {equipoEditando
                        ? `Registro ${equipoEditando.folio}`
                        : 'Recepción de equipo'}
                </DialogTitle>

                <form onSubmit={handleSubmit(guardar)} noValidate>
                    <DialogContent dividers>
                        <Stack spacing={3}>
                            <Box>
                                <Typography variant="overline" color="text.secondary">
                                    Quién entrega el equipo
                                </Typography>
                                <Stack spacing={2.5} sx={{ mt: 1 }}>
                                    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2.5}>
                                        <CampoFormulario
                                            control={control}
                                            nombre="solicitanteNombre"
                                            etiqueta="Nombre completo"
                                            obligatorio
                                            maximo={200}
                                            mayusculas
                                        />
                                        <CampoFormulario
                                            control={control}
                                            nombre="solicitanteNumero"
                                            etiqueta="Número de empleado"
                                            maximo={50}
                                            mayusculas
                                        />
                                    </Stack>
                                    <CampoFormulario
                                        control={control}
                                        nombre="departamento"
                                        etiqueta="Departamento"
                                        maximo={150}
                                        mayusculas
                                    />
                                </Stack>
                            </Box>

                            <Divider />

                            <Box>
                                <Typography variant="overline" color="text.secondary">
                                    Datos del equipo
                                </Typography>
                                <Stack spacing={2.5} sx={{ mt: 1 }}>
                                    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2.5}>
                                        <CampoFormulario
                                            control={control}
                                            nombre="equipoTipo"
                                            etiqueta="Tipo de equipo"
                                            obligatorio
                                            opciones={TIPOS_EQUIPO}
                                        />
                                        <CampoFormulario
                                            control={control}
                                            nombre="marca"
                                            etiqueta="Marca"
                                            maximo={100}
                                            mayusculas
                                        />
                                        <CampoFormulario
                                            control={control}
                                            nombre="modelo"
                                            etiqueta="Modelo"
                                            maximo={100}
                                            mayusculas
                                        />
                                    </Stack>
                                    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2.5}>
                                        <CampoFormulario
                                            control={control}
                                            nombre="numeroSerie"
                                            etiqueta="Número de serie"
                                            maximo={100}
                                            mayusculas
                                        />
                                        <CampoFormulario
                                            control={control}
                                            nombre="numeroInventario"
                                            etiqueta="Número de inventario"
                                            maximo={100}
                                            mayusculas
                                        />
                                    </Stack>
                                    <CampoFormulario
                                        control={control}
                                        nombre="accesorios"
                                        etiqueta="Accesorios recibidos"
                                        maximo={500}
                                        mayusculas
                                        ayuda="Cargador, cables, maletín…"
                                    />
                                    <CampoFormulario
                                        control={control}
                                        nombre="condicionRecepcion"
                                        etiqueta="Condición al recibirlo"
                                        maximo={500}
                                        mayusculas
                                        ayuda="Golpes, rayones o faltantes visibles al momento de la entrega."
                                    />
                                </Stack>
                            </Box>

                            <Divider />

                            <Box>
                                <Typography variant="overline" color="text.secondary">
                                    Atención técnica
                                </Typography>
                                <Stack spacing={2.5} sx={{ mt: 1 }}>
                                    <CampoFormulario
                                        control={control}
                                        nombre="fallaReportada"
                                        etiqueta="Falla reportada"
                                        obligatorio
                                        maximo={1000}
                                        mayusculas
                                        multiline
                                        rows={2}
                                        ayuda="Lo que describe quien entrega el equipo."
                                    />
                                    <CampoFormulario
                                        control={control}
                                        nombre="diagnostico"
                                        etiqueta="Diagnóstico"
                                        maximo={1000}
                                        mayusculas
                                        multiline
                                        rows={2}
                                        ayuda="Se completa al revisar el equipo."
                                    />
                                    <CampoFormulario
                                        control={control}
                                        nombre="solucion"
                                        etiqueta="Solución aplicada"
                                        maximo={1000}
                                        mayusculas
                                        multiline
                                        rows={2}
                                        ayuda="Qué se hizo para dejarlo funcionando."
                                    />
                                </Stack>
                            </Box>
                        </Stack>
                    </DialogContent>

                    <DialogActions
                        sx={{ flexDirection: { xs: 'column-reverse', sm: 'row' }, gap: 1, p: 2 }}
                    >
                        <Button
                            onClick={() => setModalAbierto(false)}
                            color="inherit"
                            disabled={isSubmitting}
                            fullWidth={esMovil}
                            size={esMovil ? 'large' : 'medium'}
                        >
                            Cancelar
                        </Button>
                        <Button
                            type="submit"
                            variant="contained"
                            disabled={isSubmitting || sinConexion}
                            fullWidth={esMovil}
                            size={esMovil ? 'large' : 'medium'}
                        >
                            {isSubmitting
                                ? 'Guardando…'
                                : equipoEditando ? 'Guardar cambios' : 'Registrar ingreso'}
                        </Button>
                    </DialogActions>
                </form>
            </Dialog>

            {/* -------------------------------------- cambio de estado ----- */}
            <Dialog
                open={Boolean(equipoCambioEstado)}
                onClose={() => !procesando && setEquipoCambioEstado(null)}
                fullWidth
                maxWidth="xs"
                fullScreen={esMovil}
                aria-labelledby="titulo-estado-taller"
            >
                <DialogTitle id="titulo-estado-taller">Cambiar estado</DialogTitle>

                <DialogContent dividers>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 2.5 }}>
                        Registro <strong>{equipoCambioEstado?.folio}</strong>. Al marcarlo
                        como entregado o irreparable, el equipo sale del taller y el
                        registro deja de admitir cambios.
                    </Typography>

                    <TextField
                        select
                        fullWidth
                        label="Nuevo estado"
                        value={nuevoEstado}
                        onChange={(e) => setNuevoEstado(e.target.value)}
                        disabled={procesando}
                    >
                        {estados.map((e) => (
                            <MenuItem key={e.valor} value={e.valor}>
                                {e.etiqueta}
                            </MenuItem>
                        ))}
                    </TextField>
                </DialogContent>

                <DialogActions
                    sx={{ flexDirection: { xs: 'column-reverse', sm: 'row' }, gap: 1, p: 2 }}
                >
                    <Button
                        onClick={() => setEquipoCambioEstado(null)}
                        color="inherit"
                        disabled={procesando}
                        fullWidth={esMovil}
                        size={esMovil ? 'large' : 'medium'}
                    >
                        Cancelar
                    </Button>
                    <Button
                        onClick={confirmarCambioEstado}
                        variant="contained"
                        disabled={procesando || sinConexion || !nuevoEstado
                            || nuevoEstado === equipoCambioEstado?.estadoTaller}
                        fullWidth={esMovil}
                        size={esMovil ? 'large' : 'medium'}
                    >
                        {procesando ? 'Guardando…' : 'Cambiar estado'}
                    </Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
}
