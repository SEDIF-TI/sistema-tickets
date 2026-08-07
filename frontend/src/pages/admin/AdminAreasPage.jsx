import { useState, useEffect, useCallback } from 'react';
import {
    Box, Typography, Button, Chip, Switch, Tooltip, Dialog, DialogTitle,
    DialogContent, DialogActions, Stack, useMediaQuery, FormControlLabel,
    Autocomplete, TextField
} from '@mui/material';
import { useTheme } from '@mui/material/styles';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DomainIcon from '@mui/icons-material/Domain';
import BlockIcon from '@mui/icons-material/Block';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutlined';
import EngineeringIcon from '@mui/icons-material/Engineering';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import { areaService } from '../../services/areaService';
import { userService } from '../../services/userService';
import { useNotification } from '../../context/NotificationContext.jsx';
import { useNetworkStatus } from '../../hooks/useNetworkStatus.jsx';
import { useTablaPaginada } from '../../hooks/useTablaPaginada.jsx';
import { esquemaArea } from '../../util/esquemas';

import DynamicTable from '../../components/DynamicTable';
import AccionesTabla from '../../components/AccionesTabla';
import ConfirmationDialog from '../../components/ConfirmationDialog';
import CampoFormulario from '../../components/CampoFormulario';

const OPCIONES_ESTADO = [
    { valor: '', etiqueta: 'Todos los estados' },
    { valor: 'true', etiqueta: 'Activas' },
    { valor: 'false', etiqueta: 'Dadas de baja' },
];

const VALORES_INICIALES = { nombre: '', prioritaria: false };

/**
 * Administración de áreas, dentro del panel del administrador.
 *
 * El catálogo de áreas alimenta los selectores de todo el sistema: usuarios,
 * avisos y el alcance de los tickets. Desde aquí se dan de alta y se editan,
 * se marcan como prioritarias, se les asigna un técnico responsable fijo y se
 * dan de baja o se reactivan.
 *
 * Las filas llegan paginadas desde `areaService.getAll()` a través de
 * `useTablaPaginada`, de modo que la búsqueda, el orden y el filtro de estado
 * los resuelve la base y no la página ya descargada. Cada columna declara su
 * `id`, su `etiqueta` y un `render` opcional que DynamicTable usa para pintar
 * la celda; sin `render` muestra el campo homónimo de la fila.
 *
 * El técnico responsable sustituye al balanceador automático para esa área:
 * si no se elige a nadie, los tickets vuelven a repartirse solos. La baja es
 * lógica, así que el mismo diálogo sirve para dar de baja y para reactivar.
 */
export default function AdminAreasPage() {
    const { notificar, notificarError } = useNotification();

    const theme = useTheme();
    const esMovil = useMediaQuery(theme.breakpoints.down('sm'));

    const estadoRed = useNetworkStatus();
    const sinConexion = Boolean(estadoRed.sinConexion ?? estadoRed);

    const [filtroEstado, setFiltroEstado] = useState('');
    const [tecnicos, setTecnicos] = useState([]);

    const [areaEditando, setAreaEditando] = useState(null);
    const [modalAbierto, setModalAbierto] = useState(false);

    const [areaSoporte, setAreaSoporte] = useState(null);
    const [tecnicoElegido, setTecnicoElegido] = useState(null);
    const [areaABaja, setAreaABaja] = useState(null);
    const [procesando, setProcesando] = useState(false);

    const cargar = useCallback((params) => areaService.getAll(params), []);

    const tabla = useTablaPaginada({
        cargar,
        ordenInicial: { campo: 'nombre', direccion: 'asc' },
        filtros: { activo: filtroEstado || undefined },
    });

    const { recargar } = tabla;

    const {
        control,
        handleSubmit,
        reset,
        formState: { isSubmitting },
    } = useForm({
        resolver: zodResolver(esquemaArea),
        mode: 'onBlur',
        defaultValues: VALORES_INICIALES,
    });

    // Catálogo para el diálogo de técnico responsable. `getSoporte()` acota la
    // consulta al personal de soporte en el servidor.
    useEffect(() => {
        let cancelado = false;

        userService.getSoporte()
            .then((respuesta) => {
                if (cancelado) return;
                const lista = Array.isArray(respuesta?.data) ? respuesta.data : [];
                // Un técnico dado de baja no puede ser responsable de un área:
                // el backend rechaza la asignación, así que no se ofrece.
                setTecnicos(lista.filter((t) => t.activo));
            })
            .catch(() => {
                if (!cancelado) setTecnicos([]);
            });

        return () => { cancelado = true; };
    }, []);

    const abrirAlta = () => {
        setAreaEditando(null);
        reset(VALORES_INICIALES);
        setModalAbierto(true);
    };

    const abrirEdicion = (area) => {
        setAreaEditando(area);
        reset({ nombre: area.nombre || '', prioritaria: Boolean(area.prioritaria) });
        setModalAbierto(true);
    };

    const guardar = async (datos) => {
        const carga = {
            nombre: datos.nombre.trim(),
            prioritaria: Boolean(datos.prioritaria),
            activo: areaEditando ? areaEditando.activo : true,
        };

        try {
            const respuesta = areaEditando
                ? await areaService.update(areaEditando.id, carga)
                : await areaService.create(carga);

            notificar(respuesta?.mensaje
                || (areaEditando ? 'Área actualizada correctamente.' : 'Área registrada correctamente.'));
            setModalAbierto(false);
            recargar();
        } catch (error) {
            notificarError(error);
        }
    };

    // El alta y la edición comparten endpoint de actualización, así que la
    // prioridad se cambia reenviando el área completa con el campo invertido.
    const alternarPrioridad = async (area) => {
        try {
            await areaService.update(area.id, {
                nombre: area.nombre,
                activo: area.activo,
                prioritaria: !area.prioritaria,
            });
            notificar(area.prioritaria
                ? `${area.nombre} deja de ser prioritaria.`
                : `${area.nombre} pasa a ser prioritaria.`);
            recargar();
        } catch (error) {
            notificarError(error);
        }
    };

    const abrirSoporte = (area) => {
        setAreaSoporte(area);
        setTecnicoElegido(
            area.soporteFijoId
                ? tecnicos.find((t) => t.id === area.soporteFijoId) ?? null
                : null
        );
    };

    const guardarSoporte = async () => {
        setProcesando(true);
        try {
            // Sin técnico elegido se retira la asignación y el área vuelve al
            // balanceador automático.
            const respuesta = await areaService.asignarSoporteFijo(
                areaSoporte.id,
                tecnicoElegido?.id ?? null
            );
            notificar(tecnicoElegido
                ? `${tecnicoElegido.nombreCompleto || tecnicoElegido.nombre} queda a cargo de ${areaSoporte.nombre}.`
                : `${areaSoporte.nombre} vuelve a la asignación automática.`);
            setAreaSoporte(null);
            recargar();
            return respuesta;
        } catch (error) {
            notificarError(error);
        } finally {
            setProcesando(false);
        }
    };

    const confirmarBaja = async () => {
        setProcesando(true);
        try {
            if (areaABaja.activo) {
                const respuesta = await areaService.delete(areaABaja.id);
                notificar(respuesta?.mensaje || 'Área dada de baja correctamente.');
            } else {
                // Reactivar es una edición normal: el endpoint de baja solo
                // desactiva.
                await areaService.update(areaABaja.id, {
                    nombre: areaABaja.nombre,
                    prioritaria: Boolean(areaABaja.prioritaria),
                    activo: true,
                });
                notificar(`${areaABaja.nombre} vuelve a estar activa.`);
            }
            setAreaABaja(null);
            recargar();
        } catch (error) {
            notificarError(error);
        } finally {
            setProcesando(false);
        }
    };

    const columnas = [
        {
            id: 'nombre',
            etiqueta: 'Área',
            principal: true,
            ordenable: true,
            render: (a) => (
                <Typography variant="body2" fontWeight={500}>
                    {a.nombre}
                </Typography>
            ),
        },
        {
            id: 'soporteFijoNombre',
            etiqueta: 'Técnico responsable',
            ancho: '26%',
            sinOrden: true,
            render: (a) => (
                a.soporteFijoId ? (
                    <Typography variant="body2">{a.soporteFijoNombre}</Typography>
                ) : (
                    <Typography variant="body2" color="text.secondary">
                        Asignación automática
                    </Typography>
                )
            ),
        },
        {
            id: 'prioritaria',
            etiqueta: 'Prioritaria',
            alineacion: 'center',
            ancho: 120,
            sinOrden: true,
            render: (a) => (
                <Tooltip
                    title={a.prioritaria
                        ? 'Sus tickets reciben atención preferente'
                        : 'Atención en el orden habitual'}
                    arrow
                >
                    <span>
                        <Switch
                            checked={Boolean(a.prioritaria)}
                            onChange={() => alternarPrioridad(a)}
                            disabled={!a.activo || sinConexion}
                            color="warning"
                            inputProps={{ 'aria-label': `Prioridad del área ${a.nombre}` }}
                        />
                    </span>
                </Tooltip>
            ),
        },
        {
            id: 'activo',
            etiqueta: 'Estado',
            alineacion: 'center',
            ancho: 120,
            render: (a) => (
                <Chip
                    label={a.activo ? 'Activa' : 'Dada de baja'}
                    size="small"
                    color={a.activo ? 'success' : 'default'}
                    variant={a.activo ? 'filled' : 'outlined'}
                />
            ),
        },
        {
            id: 'acciones',
            etiqueta: 'Acciones',
            alineacion: 'center',
            ancho: 140,
            sinOrden: true,
            render: (a) => (
                <AccionesTabla
                    acciones={[
                        {
                            id: 'editar',
                            icono: <EditIcon />,
                            titulo: 'Editar',
                            etiqueta: `Editar el área ${a.nombre}`,
                            onClick: () => abrirEdicion(a),
                            deshabilitada: sinConexion,
                            motivoDeshabilitada: 'Sin conexión con el servidor',
                        },
                        {
                            id: 'soporte',
                            icono: <EngineeringIcon />,
                            titulo: 'Técnico responsable',
                            etiqueta: `Asignar el técnico responsable de ${a.nombre}`,
                            onClick: () => abrirSoporte(a),
                            deshabilitada: sinConexion || !a.activo,
                            motivoDeshabilitada: !a.activo
                                ? 'El área está dada de baja'
                                : 'Sin conexión con el servidor',
                        },
                        {
                            id: 'estado',
                            icono: a.activo ? <BlockIcon /> : <CheckCircleOutlineIcon />,
                            titulo: a.activo ? 'Dar de baja' : 'Reactivar',
                            etiqueta: `${a.activo ? 'Dar de baja el área' : 'Reactivar el área'} ${a.nombre}`,
                            color: a.activo ? 'error' : 'success',
                            onClick: () => setAreaABaja(a),
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
                        <DomainIcon fontSize="large" aria-hidden="true" />
                        Áreas
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        Unidades administrativas y su técnico responsable.
                    </Typography>
                </Box>

                <Button
                    variant="contained"
                    startIcon={<AddIcon />}
                    onClick={abrirAlta}
                    disabled={sinConexion}
                >
                    Nueva área
                </Button>
            </Box>

            <DynamicTable
                columnas={columnas}
                filas={tabla.filas}
                cargando={tabla.cargando}
                anchoMinimo={900}
                busqueda={tabla.busqueda}
                onBuscar={tabla.setBusqueda}
                placeholderBusqueda="Buscar área por nombre…"
                filtros={[
                    {
                        id: 'estado',
                        etiqueta: 'Estado',
                        valor: filtroEstado,
                        valorPorDefecto: '',
                        opciones: OPCIONES_ESTADO,
                        onChange: setFiltroEstado,
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
                    icono: DomainIcon,
                    titulo: tabla.busqueda || filtroEstado
                        ? 'Sin resultados'
                        : 'No hay áreas registradas',
                    descripcion: tabla.busqueda || filtroEstado
                        ? 'Prueba con otro nombre o cambia el filtro de estado.'
                        : 'Da de alta las unidades administrativas de la institución.',
                    textoAccion: !tabla.busqueda && !filtroEstado ? 'Crear la primera área' : undefined,
                    onAccion: !tabla.busqueda && !filtroEstado ? abrirAlta : undefined,
                }}
            />

            <Dialog
                open={modalAbierto}
                onClose={() => !isSubmitting && setModalAbierto(false)}
                fullWidth
                maxWidth="sm"
                fullScreen={esMovil}
                aria-labelledby="titulo-area"
            >
                <DialogTitle id="titulo-area">
                    {areaEditando ? 'Editar área' : 'Nueva área'}
                </DialogTitle>

                <form onSubmit={handleSubmit(guardar)} noValidate>
                    <DialogContent dividers>
                        <Stack spacing={2.5}>
                            <CampoFormulario
                                control={control}
                                nombre="nombre"
                                etiqueta="Nombre del área"
                                obligatorio
                                maximo={100}
                                mayusculas
                                ayuda="Como aparecerá en los selectores y en los documentos."
                            />

                            <Controller
                                name="prioritaria"
                                control={control}
                                render={({ field }) => (
                                    <FormControlLabel
                                        control={
                                            <Switch
                                                checked={Boolean(field.value)}
                                                onChange={(e) => field.onChange(e.target.checked)}
                                                color="warning"
                                            />
                                        }
                                        label={
                                            <Box>
                                                <Typography variant="body2">Área prioritaria</Typography>
                                                <Typography variant="caption" color="text.secondary">
                                                    Sus tickets reciben atención preferente.
                                                </Typography>
                                            </Box>
                                        }
                                    />
                                )}
                            />
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
                            {isSubmitting ? 'Guardando…' : areaEditando ? 'Guardar cambios' : 'Crear área'}
                        </Button>
                    </DialogActions>
                </form>
            </Dialog>

            <Dialog
                open={Boolean(areaSoporte)}
                onClose={() => !procesando && setAreaSoporte(null)}
                fullWidth
                maxWidth="xs"
                fullScreen={esMovil}
                aria-labelledby="titulo-soporte"
            >
                <DialogTitle id="titulo-soporte">Técnico responsable</DialogTitle>

                <DialogContent dividers>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 2.5 }}>
                        Quien atienda de forma fija los tickets de{' '}
                        <strong>{areaSoporte?.nombre}</strong>. Si no eliges a nadie, los
                        tickets se reparten automáticamente entre el personal disponible.
                    </Typography>

                    <Autocomplete
                        options={tecnicos}
                        value={tecnicoElegido}
                        onChange={(_e, valor) => setTecnicoElegido(valor)}
                        getOptionLabel={(t) => t.nombreCompleto || t.nombre || ''}
                        isOptionEqualToValue={(opcion, valor) => opcion.id === valor.id}
                        noOptionsText="No hay personal de soporte activo"
                        renderInput={(params) => (
                            <TextField
                                {...params}
                                label="Técnico de soporte"
                                helperText="Déjalo vacío para volver a la asignación automática."
                            />
                        )}
                    />
                </DialogContent>

                <DialogActions
                    sx={{ flexDirection: { xs: 'column-reverse', sm: 'row' }, gap: 1, p: 2 }}
                >
                    <Button
                        onClick={() => setAreaSoporte(null)}
                        color="inherit"
                        disabled={procesando}
                        fullWidth={esMovil}
                        size={esMovil ? 'large' : 'medium'}
                    >
                        Cancelar
                    </Button>
                    <Button
                        onClick={guardarSoporte}
                        variant="contained"
                        disabled={procesando || sinConexion}
                        fullWidth={esMovil}
                        size={esMovil ? 'large' : 'medium'}
                    >
                        {procesando ? 'Guardando…' : 'Guardar'}
                    </Button>
                </DialogActions>
            </Dialog>

            <ConfirmationDialog
                abierto={Boolean(areaABaja)}
                titulo={areaABaja?.activo ? '¿Dar de baja esta área?' : '¿Reactivar esta área?'}
                mensaje={areaABaja?.activo
                    ? `${areaABaja?.nombre} dejará de aparecer en los formularios. Los tickets ya registrados conservan su área. Si todavía tiene personal asignado, el sistema no permitirá la baja.`
                    : `${areaABaja?.nombre} volverá a estar disponible en los formularios.`}
                textoConfirmar={areaABaja?.activo ? 'Sí, dar de baja' : 'Sí, reactivar'}
                destructivo={areaABaja?.activo}
                cargando={procesando}
                onConfirmar={confirmarBaja}
                onCancelar={() => setAreaABaja(null)}
            />
        </Box>
    );
}
