import { useState, useEffect, useCallback } from 'react';
import {
    Box, Typography, Button, Chip, Switch, Tooltip, Dialog, DialogTitle,
    DialogContent, DialogActions, Stack, useMediaQuery
} from '@mui/material';
import { useTheme } from '@mui/material/styles';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import CampaignIcon from '@mui/icons-material/Campaign';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutlineOutlined';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import { avisoService } from '../../services/avisoService';
import { areaService } from '../../services/areaService';
import { useNotification } from '../../context/NotificationContext.jsx';
import { useNetworkStatus } from '../../hooks/useNetworkStatus.jsx';
import { esquemaAviso } from '../../util/esquemas';
import { truncar } from '../../util/formater';

import DynamicTable from '../../components/DynamicTable';
import AccionesTabla from '../../components/AccionesTabla';
import ConfirmationDialog from '../../components/ConfirmationDialog';
import CampoFormulario from '../../components/CampoFormulario';

const VALORES_INICIALES = { titulo: '', mensaje: '', areaId: '' };

/**
 * Administración de avisos, dentro del panel del administrador.
 *
 * Un aviso es el mensaje que aparece en la parte superior del panel de cada
 * usuario. Desde aquí se publican, se editan, se ocultan sin borrarlos con el
 * interruptor de visibilidad y se eliminan de forma definitiva.
 *
 * El alcance lo decide `areaId`: sin área el aviso lo ve toda la institución;
 * con área, solo su personal. El nombre a mostrar viene resuelto del backend
 * en `areaNombre`, sin cruzarlo contra el catálogo local.
 *
 * Las columnas siguen el contrato de DynamicTable: `id`, `etiqueta` y un
 * `render` opcional que dibuja la celda a partir de la fila completa.
 */
export default function AdminAvisosPage() {
    const { notificar, notificarError } = useNotification();

    const theme = useTheme();
    const esMovil = useMediaQuery(theme.breakpoints.down('sm'));

    const estadoRed = useNetworkStatus();
    const sinConexion = Boolean(estadoRed.sinConexion ?? estadoRed);

    const [areas, setAreas] = useState([]);
    const [avisos, setAvisos] = useState([]);
    const [cargando, setCargando] = useState(true);

    const [avisoEditando, setAvisoEditando] = useState(null);
    const [modalAbierto, setModalAbierto] = useState(false);
    const [avisoABorrar, setAvisoABorrar] = useState(null);
    const [procesando, setProcesando] = useState(false);

    const {
        control,
        handleSubmit,
        reset,
        formState: { isSubmitting },
    } = useForm({
        resolver: zodResolver(esquemaAviso),
        mode: 'onBlur',
        defaultValues: VALORES_INICIALES,
    });

    // El endpoint de avisos no está paginado, a diferencia del resto del panel:
    // son pocos por definición, ya que todos se muestran a la vez en la barra
    // superior de cada usuario. De ahí que la tabla reciba el array completo en
    // lugar de apoyarse en useTablaPaginada.
    //
    // `mostrarCarga` permite recargar tras una acción sin vaciar la tabla: el
    // esqueleto de carga en mitad de una edición produce un parpadeo molesto.
    const cargarAvisos = useCallback(async (mostrarCarga = false) => {
        if (mostrarCarga) setCargando(true);
        try {
            const respuesta = await avisoService.getAll();
            setAvisos(Array.isArray(respuesta?.data) ? respuesta.data : []);
        } catch (error) {
            notificarError(error);
            setAvisos([]);
        } finally {
            setCargando(false);
        }
    }, [notificarError]);

    useEffect(() => {
        // La carga inicial no pasa `mostrarCarga`: `cargando` ya empieza en
        // true, así que el esqueleto se pinta sin un cambio de estado extra.
        cargarAvisos();
    }, [cargarAvisos]);

    useEffect(() => {
        let cancelado = false;

        areaService.getTodas()
            .then((respuesta) => {
                if (cancelado) return;
                const lista = Array.isArray(respuesta?.data) ? respuesta.data : [];
                // Un aviso dirigido a un área dada de baja no lo vería nadie.
                setAreas(lista.filter((a) => a.activo));
            })
            .catch(() => {
                if (!cancelado) setAreas([]);
            });

        return () => { cancelado = true; };
    }, []);

    const abrirAlta = () => {
        setAvisoEditando(null);
        reset(VALORES_INICIALES);
        setModalAbierto(true);
    };

    const abrirEdicion = (aviso) => {
        setAvisoEditando(aviso);
        reset({
            titulo: aviso.titulo || '',
            mensaje: aviso.mensaje || '',
            areaId: aviso.areaId ? String(aviso.areaId) : '',
        });
        setModalAbierto(true);
    };

    const guardar = async (datos) => {
        const carga = {
            titulo: datos.titulo.trim(),
            mensaje: datos.mensaje.trim(),
            areaId: datos.areaId ? Number(datos.areaId) : null,
            activo: avisoEditando ? avisoEditando.activo : true,
        };

        try {
            const respuesta = avisoEditando
                ? await avisoService.update(avisoEditando.id, carga)
                : await avisoService.create(carga);

            notificar(respuesta?.mensaje
                || (avisoEditando ? 'Aviso actualizado correctamente.' : 'Aviso publicado correctamente.'));
            setModalAbierto(false);
            cargarAvisos();
        } catch (error) {
            notificarError(error);
        }
    };

    const alternarEstado = async (aviso, activo) => {
        try {
            await avisoService.update(aviso.id, {
                titulo: aviso.titulo,
                mensaje: aviso.mensaje,
                areaId: aviso.areaId,
                activo,
            });
            notificar(activo
                ? 'El aviso vuelve a mostrarse en los paneles.'
                : 'El aviso deja de mostrarse en los paneles.');
            cargarAvisos();
        } catch (error) {
            notificarError(error);
        }
    };

    const confirmarBorrado = async () => {
        setProcesando(true);
        try {
            const respuesta = await avisoService.delete(avisoABorrar.id);
            notificar(respuesta?.mensaje || 'Aviso eliminado correctamente.');
            setAvisoABorrar(null);
            cargarAvisos();
        } catch (error) {
            notificarError(error);
        } finally {
            setProcesando(false);
        }
    };

    const columnas = [
        {
            id: 'titulo',
            etiqueta: 'Título',
            principal: true,
            ancho: '22%',
            render: (a) => (
                <Typography variant="body2" fontWeight={500}>
                    {truncar(a.titulo, 40)}
                </Typography>
            ),
        },
        {
            id: 'mensaje',
            etiqueta: 'Mensaje',
            render: (a) => (
                <Tooltip title={a.mensaje || ''} arrow placement="top-start">
                    <Typography variant="body2" color="text.secondary">
                        {truncar(a.mensaje, 70)}
                    </Typography>
                </Tooltip>
            ),
        },
        {
            id: 'areaNombre',
            etiqueta: 'Alcance',
            ancho: '18%',
            render: (a) => (
                <Chip
                    label={a.areaId ? (a.areaNombre || 'Área específica') : 'Toda la institución'}
                    size="small"
                    color={a.areaId ? 'default' : 'primary'}
                    variant="outlined"
                />
            ),
        },
        {
            id: 'activo',
            etiqueta: 'Visible',
            alineacion: 'center',
            ancho: 110,
            render: (a) => (
                <Tooltip
                    title={a.activo ? 'Se muestra en los paneles' : 'Oculto para los usuarios'}
                    arrow
                >
                    <span>
                        <Switch
                            checked={Boolean(a.activo)}
                            onChange={(e) => alternarEstado(a, e.target.checked)}
                            disabled={sinConexion}
                            color="success"
                            inputProps={{ 'aria-label': `Visibilidad del aviso ${a.titulo}` }}
                        />
                    </span>
                </Tooltip>
            ),
        },
        {
            id: 'acciones',
            etiqueta: 'Acciones',
            alineacion: 'center',
            ancho: 110,
            sinOrden: true,
            render: (a) => (
                <AccionesTabla
                    acciones={[
                        {
                            id: 'editar',
                            icono: <EditIcon />,
                            titulo: 'Editar',
                            etiqueta: `Editar el aviso ${a.titulo}`,
                            onClick: () => abrirEdicion(a),
                            deshabilitada: sinConexion,
                            motivoDeshabilitada: 'Sin conexión con el servidor',
                        },
                        {
                            id: 'borrar',
                            icono: <DeleteOutlineIcon />,
                            titulo: 'Eliminar',
                            etiqueta: `Eliminar el aviso ${a.titulo}`,
                            color: 'error',
                            onClick: () => setAvisoABorrar(a),
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
                        <CampaignIcon fontSize="large" aria-hidden="true" />
                        Avisos
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        Mensajes que aparecen en la parte superior del panel de cada usuario.
                    </Typography>
                </Box>

                <Button
                    variant="contained"
                    startIcon={<AddIcon />}
                    onClick={abrirAlta}
                    disabled={sinConexion}
                >
                    Nuevo aviso
                </Button>
            </Box>

            <DynamicTable
                columnas={columnas}
                filas={avisos}
                cargando={cargando}
                anchoMinimo={900}
                onRecargar={() => cargarAvisos(true)}
                vacio={{
                    icono: CampaignIcon,
                    titulo: 'No hay avisos publicados',
                    descripcion: 'Los avisos aparecen en la parte superior del panel de cada usuario, útiles para comunicar mantenimientos o incidencias generales.',
                    textoAccion: 'Publicar el primer aviso',
                    onAccion: abrirAlta,
                }}
            />

            <Dialog
                open={modalAbierto}
                onClose={() => !isSubmitting && setModalAbierto(false)}
                fullWidth
                maxWidth="sm"
                fullScreen={esMovil}
                aria-labelledby="titulo-aviso"
            >
                <DialogTitle id="titulo-aviso">
                    {avisoEditando ? 'Editar aviso' : 'Nuevo aviso'}
                </DialogTitle>

                <form onSubmit={handleSubmit(guardar)} noValidate>
                    <DialogContent dividers>
                        <Stack spacing={2.5}>
                            <CampoFormulario
                                control={control}
                                nombre="titulo"
                                etiqueta="Título"
                                obligatorio
                                maximo={150}
                                mayusculas
                                ayuda="Resume el aviso en pocas palabras."
                            />

                            <CampoFormulario
                                control={control}
                                nombre="mensaje"
                                etiqueta="Mensaje"
                                obligatorio
                                maximo={2000}
                                multiline
                                rows={4}
                                ayuda="Lo que leerán los usuarios en su panel."
                            />

                            <CampoFormulario
                                control={control}
                                nombre="areaId"
                                etiqueta="Dirigido a"
                                opciones={[
                                    { valor: '', etiqueta: 'Toda la institución' },
                                    ...areas.map((a) => ({
                                        valor: String(a.id),
                                        etiqueta: a.nombre,
                                    })),
                                ]}
                                ayuda="Elige un área para que solo la vea su personal."
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
                            {isSubmitting ? 'Guardando…' : avisoEditando ? 'Guardar cambios' : 'Publicar aviso'}
                        </Button>
                    </DialogActions>
                </form>
            </Dialog>

            <ConfirmationDialog
                abierto={Boolean(avisoABorrar)}
                titulo="¿Eliminar este aviso?"
                mensaje={`"${avisoABorrar?.titulo}" se eliminará de forma permanente y no se podrá recuperar. Si solo quieres dejar de mostrarlo, apaga su interruptor de visibilidad.`}
                textoConfirmar="Sí, eliminar"
                destructivo
                cargando={procesando}
                onConfirmar={confirmarBorrado}
                onCancelar={() => setAvisoABorrar(null)}
            />
        </Box>
    );
}
