import { useState, useCallback } from 'react';
import {
    Box, Typography, Button, Dialog, DialogTitle, DialogContent, DialogActions,
    Stack, useMediaQuery
} from '@mui/material';
import { useTheme } from '@mui/material/styles';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import ComputerIcon from '@mui/icons-material/Computer';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutlineOutlined';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import { equipoService } from '../../services/equipoService';
import { useNotification } from '../../context/NotificationContext.jsx';
import { useNetworkStatus } from '../../hooks/useNetworkStatus.jsx';
import { useTablaPaginada } from '../../hooks/useTablaPaginada.jsx';
import { esquemaEquipo } from '../../util/esquemas';
import { formatearFecha } from '../../util/formater';

import DynamicTable from '../../components/DynamicTable';
import AccionesTabla from '../../components/AccionesTabla';
import ConfirmationDialog from '../../components/ConfirmationDialog';
import CampoFormulario from '../../components/CampoFormulario';

const VALORES_INICIALES = { descripcion: '', marca: '', modelo: '' };

/**
 * Catálogo de equipos.
 *
 * Alimenta el autocompletado de los dictámenes técnicos: registrar aquí un
 * equipo evita volver a teclear marca y modelo en cada reporte.
 *
 * Cambios respecto a la versión anterior:
 *  - Editar llamaba a `/api/v1/equipos/{id}` con el `baseURL` que ya incluye
 *    `/api`, así que la URL final era `/api/api/v1/…`: siempre 404. La edición
 *    nunca llegó a funcionar.
 *  - La tabla traía el catálogo completo y filtraba en el navegador, y el
 *    endpoint ya devuelve una página: el `.filter()` habría fallado.
 *  - Los errores se mostraban con `alert()` incluyendo el volcado del backend
 *    ("El servidor rechazó la petición. Motivo real: …"), que expone detalles
 *    internos al usuario.
 *  - `window.confirm()` para borrar, sin decir qué equipo.
 */
export default function AdminEquiposPage() {
    const { notificar, notificarError } = useNotification();

    const theme = useTheme();
    const esMovil = useMediaQuery(theme.breakpoints.down('sm'));

    const estadoRed = useNetworkStatus();
    const sinConexion = Boolean(estadoRed.sinConexion ?? estadoRed);

    const [equipoEditando, setEquipoEditando] = useState(null);
    const [modalAbierto, setModalAbierto] = useState(false);
    const [equipoABorrar, setEquipoABorrar] = useState(null);
    const [procesando, setProcesando] = useState(false);

    const cargar = useCallback((params) => equipoService.getAll(params), []);

    const tabla = useTablaPaginada({
        cargar,
        ordenInicial: { campo: 'descripcion', direccion: 'asc' },
    });

    const { recargar } = tabla;

    const {
        control,
        handleSubmit,
        reset,
        formState: { isSubmitting },
    } = useForm({
        resolver: zodResolver(esquemaEquipo),
        mode: 'onBlur',
        defaultValues: VALORES_INICIALES,
    });

    const abrirAlta = () => {
        setEquipoEditando(null);
        reset(VALORES_INICIALES);
        setModalAbierto(true);
    };

    const abrirEdicion = (equipo) => {
        setEquipoEditando(equipo);
        reset({
            descripcion: equipo.descripcion || '',
            marca: equipo.marca || '',
            modelo: equipo.modelo || '',
        });
        setModalAbierto(true);
    };

    const guardar = async (datos) => {
        const carga = {
            descripcion: datos.descripcion.trim(),
            marca: datos.marca?.trim() || null,
            modelo: datos.modelo?.trim() || null,
        };

        try {
            const respuesta = equipoEditando
                ? await equipoService.update(equipoEditando.id, carga)
                : await equipoService.create(carga);

            notificar(respuesta?.mensaje
                || (equipoEditando ? 'Equipo actualizado.' : 'Equipo agregado al catálogo.'));
            setModalAbierto(false);
            recargar();
        } catch (error) {
            notificarError(error);
        }
    };

    const confirmarBorrado = async () => {
        setProcesando(true);
        try {
            const respuesta = await equipoService.delete(equipoABorrar.id);
            notificar(respuesta?.mensaje || 'Equipo eliminado del catálogo.');
            setEquipoABorrar(null);
            recargar();
        } catch (error) {
            notificarError(error);
        } finally {
            setProcesando(false);
        }
    };

    const columnas = [
        {
            id: 'descripcion',
            etiqueta: 'Descripción',
            principal: true,
            ordenable: true,
            render: (e) => (
                <Typography variant="body2" fontWeight={500}>
                    {e.descripcion}
                </Typography>
            ),
        },
        {
            id: 'marca',
            etiqueta: 'Marca',
            ancho: '20%',
            ordenable: true,
            render: (e) => (
                <Typography variant="body2" color="text.secondary">
                    {e.marca || '—'}
                </Typography>
            ),
        },
        {
            id: 'modelo',
            etiqueta: 'Modelo',
            ancho: '20%',
            ordenable: true,
            render: (e) => (
                <Typography variant="body2" color="text.secondary">
                    {e.modelo || '—'}
                </Typography>
            ),
        },
        {
            id: 'fechaRegistro',
            etiqueta: 'Registrado',
            ancho: 140,
            ordenable: true,
            sx: { whiteSpace: 'nowrap' },
            render: (e) => (
                <Typography variant="body2" color="text.secondary">
                    {formatearFecha(e.fechaRegistro)}
                </Typography>
            ),
        },
        {
            id: 'acciones',
            etiqueta: 'Acciones',
            alineacion: 'center',
            ancho: 110,
            sinOrden: true,
            render: (e) => (
                <AccionesTabla
                    acciones={[
                        {
                            id: 'editar',
                            icono: <EditIcon />,
                            titulo: 'Editar',
                            etiqueta: `Editar el equipo ${e.descripcion}`,
                            onClick: () => abrirEdicion(e),
                            deshabilitada: sinConexion,
                            motivoDeshabilitada: 'Sin conexión con el servidor',
                        },
                        {
                            id: 'borrar',
                            icono: <DeleteOutlineIcon />,
                            titulo: 'Eliminar',
                            etiqueta: `Eliminar el equipo ${e.descripcion} del catálogo`,
                            color: 'error',
                            onClick: () => setEquipoABorrar(e),
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
                        <ComputerIcon fontSize="large" aria-hidden="true" />
                        Catálogo de equipos
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        Modelos frecuentes, para no volver a capturarlos en cada dictamen.
                    </Typography>
                </Box>

                <Button
                    variant="contained"
                    startIcon={<AddIcon />}
                    onClick={abrirAlta}
                    disabled={sinConexion}
                >
                    Nuevo equipo
                </Button>
            </Box>

            <DynamicTable
                columnas={columnas}
                filas={tabla.filas}
                cargando={tabla.cargando}
                anchoMinimo={900}
                busqueda={tabla.busqueda}
                onBuscar={tabla.setBusqueda}
                placeholderBusqueda="Buscar por descripción, marca o modelo…"
                paginacion={tabla.paginacion}
                onCambiarPagina={tabla.cambiarPagina}
                onCambiarTamano={tabla.cambiarTamano}
                orden={tabla.orden}
                onCambiarOrden={tabla.cambiarOrden}
                onRecargar={recargar}
                vacio={{
                    icono: ComputerIcon,
                    titulo: tabla.busqueda ? 'Sin resultados' : 'El catálogo está vacío',
                    descripcion: tabla.busqueda
                        ? 'Prueba con otra descripción, marca o modelo.'
                        : 'Registra los equipos que se atienden con más frecuencia para agilizar los dictámenes.',
                    textoAccion: !tabla.busqueda ? 'Agregar el primer equipo' : undefined,
                    onAccion: !tabla.busqueda ? abrirAlta : undefined,
                }}
            />

            <Dialog
                open={modalAbierto}
                onClose={() => !isSubmitting && setModalAbierto(false)}
                fullWidth
                maxWidth="sm"
                fullScreen={esMovil}
                aria-labelledby="titulo-equipo"
            >
                <DialogTitle id="titulo-equipo">
                    {equipoEditando ? 'Editar equipo' : 'Nuevo equipo'}
                </DialogTitle>

                <form onSubmit={handleSubmit(guardar)} noValidate>
                    <DialogContent dividers>
                        <Stack spacing={2.5}>
                            <CampoFormulario
                                control={control}
                                nombre="descripcion"
                                etiqueta="Descripción"
                                obligatorio
                                maximo={255}
                                mayusculas
                                ayuda="Cómo se identifica el equipo, por ejemplo LAPTOP DELL LATITUDE."
                            />

                            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2.5}>
                                <CampoFormulario
                                    control={control}
                                    nombre="marca"
                                    etiqueta="Marca"
                                    maximo={255}
                                    mayusculas
                                />
                                <CampoFormulario
                                    control={control}
                                    nombre="modelo"
                                    etiqueta="Modelo"
                                    maximo={255}
                                    mayusculas
                                />
                            </Stack>
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
                            {isSubmitting ? 'Guardando…' : equipoEditando ? 'Guardar cambios' : 'Agregar equipo'}
                        </Button>
                    </DialogActions>
                </form>
            </Dialog>

            <ConfirmationDialog
                abierto={Boolean(equipoABorrar)}
                titulo="¿Eliminar este equipo del catálogo?"
                mensaje={`"${equipoABorrar?.descripcion}" dejará de ofrecerse como sugerencia al capturar un dictamen. Los dictámenes ya emitidos conservan sus datos.`}
                textoConfirmar="Sí, eliminar"
                destructivo
                cargando={procesando}
                onConfirmar={confirmarBorrado}
                onCancelar={() => setEquipoABorrar(null)}
            />
        </Box>
    );
}
