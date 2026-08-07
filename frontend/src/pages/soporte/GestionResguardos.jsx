import { useState, useCallback } from 'react';
import {
    Box, Typography, Button, Chip, Tooltip, Dialog, DialogTitle, DialogContent,
    DialogActions, Stack, useMediaQuery, Divider
} from '@mui/material';
import { useTheme } from '@mui/material/styles';
import AddIcon from '@mui/icons-material/Add';
import InventoryIcon from '@mui/icons-material/Inventory';
import AssignmentReturnIcon from '@mui/icons-material/AssignmentReturn';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import { useForm, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import api from '../../services/api';
import { resguardoService } from '../../services/resguardoService';
import { useNotification } from '../../context/NotificationContext.jsx';
import { useNetworkStatus } from '../../hooks/useNetworkStatus.jsx';
import { useTablaPaginada } from '../../hooks/useTablaPaginada.jsx';
import { esquemaResguardo } from '../../util/esquemas';
import { formatearFecha, diasHasta, truncar } from '../../util/formater';
import {
    OPCIONES_ESTADO_RESGUARDO, colorEstadoResguardo, etiquetaEstadoResguardo, estaVigente,
} from '../../util/estadoResguardo';

import DynamicTable from '../../components/DynamicTable';
import AccionesTabla from '../../components/AccionesTabla';
import ConfirmationDialog from '../../components/ConfirmationDialog';
import CampoFormulario from '../../components/CampoFormulario';

/** Unidades de plazo que acepta el backend (ver `ResguardoRequest`). */
const OPCIONES_DURACION = [
    { valor: 'dias', etiqueta: 'Días' },
    { valor: 'semanas', etiqueta: 'Semanas' },
    { valor: 'meses', etiqueta: 'Meses' },
    { valor: 'indefinido', etiqueta: 'Sin plazo definido' },
];

const VALORES_INICIALES = {
    solicitanteNombre: '',
    solicitanteNumero: '',
    departamento: '',
    telefono: '',
    equipoNombre: '',
    numeroSerie: '',
    numeroInventario: '',
    condiciones: 'BUENO',
    accesorios: '',
    duracionTipo: 'dias',
    duracionCantidad: '30',
};

/**
 * Resguardos: equipo entregado en préstamo al personal.
 *
 * El alta registra a quién se entrega el equipo, cómo se identifica (serie e
 * inventario), en qué condiciones sale y qué accesorios lo acompañan. La
 * vigencia se expresa como cantidad más unidad —días, semanas o meses— y el
 * backend calcula con ella la fecha de vencimiento; con "sin plazo definido"
 * el resguardo queda abierto y la cantidad no se pide.
 *
 * Un resguardo vigente permanece así hasta que se registra la devolución, que
 * se confirma antes de ejecutarse porque cierra el préstamo. La columna
 * "Vence" resalta en rojo los que caducan en siete días o menos, y omite el
 * aviso en los ya devueltos aunque su fecha haya pasado.
 *
 * La responsiva se genera en `POST /v1/documentos/resguardos`: el servidor
 * devuelve el PDF como blob y se abre en una pestaña nueva para firmarlo.
 *
 * El listado se pagina, ordena, busca y filtra en el servidor.
 */
export default function GestionResguardos() {
    const { notificar, notificarError } = useNotification();

    const theme = useTheme();
    const esMovil = useMediaQuery(theme.breakpoints.down('sm'));

    const estadoRed = useNetworkStatus();
    const sinConexion = Boolean(estadoRed.sinConexion ?? estadoRed);

    const [filtroEstado, setFiltroEstado] = useState('');
    const [modalAbierto, setModalAbierto] = useState(false);
    const [resguardoADevolver, setResguardoADevolver] = useState(null);
    const [procesando, setProcesando] = useState(false);

    const cargar = useCallback((params) => resguardoService.getAll(params), []);

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
        resolver: zodResolver(esquemaResguardo),
        mode: 'onBlur',
        defaultValues: VALORES_INICIALES,
    });

    // Con plazo indefinido la cantidad sobra: se oculta en lugar de dejar un
    // campo que no influye en nada.
    const duracionTipo = useWatch({ control, name: 'duracionTipo' });
    const exigePlazo = duracionTipo !== 'indefinido';

    const abrirAlta = () => {
        reset(VALORES_INICIALES);
        setModalAbierto(true);
    };

    const guardar = async (datos) => {
        try {
            const respuesta = await resguardoService.create({
                solicitanteNombre: datos.solicitanteNombre.trim(),
                solicitanteNumero: datos.solicitanteNumero.trim(),
                departamento: datos.departamento?.trim() || null,
                telefono: datos.telefono?.trim() || null,
                equipoNombre: datos.equipoNombre.trim(),
                numeroSerie: datos.numeroSerie.trim(),
                numeroInventario: datos.numeroInventario?.trim() || null,
                condiciones: datos.condiciones?.trim() || null,
                accesorios: datos.accesorios?.trim() || null,
                duracionTipo: datos.duracionTipo,
                duracionCantidad: exigePlazo ? Number(datos.duracionCantidad) : null,
            });

            notificar(respuesta?.mensaje || 'Resguardo registrado correctamente.');
            setModalAbierto(false);
            recargar();
        } catch (error) {
            notificarError(error);
        }
    };

    const confirmarDevolucion = async () => {
        setProcesando(true);
        try {
            const respuesta = await resguardoService.devolver(resguardoADevolver.id);
            notificar(respuesta?.mensaje || 'Devolución registrada correctamente.');
            setResguardoADevolver(null);
            recargar();
        } catch (error) {
            notificarError(error);
        } finally {
            setProcesando(false);
        }
    };

    // Responsiva del resguardo: el servidor arma el PDF a partir del registro
    // y se abre en una pestaña para imprimirlo y recabar la firma.
    const imprimir = async (resguardo) => {
        try {
            const respuesta = await api.post('/v1/documentos/resguardos', resguardo, {
                responseType: 'blob',
            });

            const url = window.URL.createObjectURL(
                new Blob([respuesta.data], { type: 'application/pdf' })
            );
            const ventana = window.open(url, '_blank');

            // Si el navegador bloquea la ventana emergente, se avisa en lugar
            // de dejar al usuario esperando un PDF que nunca aparece.
            if (!ventana) {
                notificarError('El navegador bloqueó la ventana. Permite las ventanas emergentes para ver el documento.');
            }

            // Se libera el objeto pasado un momento: revocarlo de inmediato
            // cancelaría la carga en la pestaña recién abierta.
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
            ancho: 150,
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
                const devuelto = !estaVigente(r.estado);

                // Un préstamo devuelto ya no urge, aunque su fecha haya pasado.
                const urgente = !devuelto && dias !== null && dias <= 7;

                return (
                    <Tooltip
                        title={devuelto
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
            ancho: 120,
            sinOrden: true,
            render: (r) => {
                const vigente = estaVigente(r.estado);

                return (
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
                            {
                                id: 'devolver',
                                icono: <AssignmentReturnIcon />,
                                titulo: 'Registrar devolución',
                                etiqueta: `Registrar la devolución del equipo de ${r.solicitanteNombre}`,
                                color: 'success',
                                prioritaria: true,
                                oculta: !vigente,
                                onClick: () => setResguardoADevolver(r),
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
                        <InventoryIcon fontSize="large" aria-hidden="true" />
                        Resguardos
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        Equipo entregado en préstamo al personal.
                    </Typography>
                </Box>

                <Button
                    variant="contained"
                    startIcon={<AddIcon />}
                    onClick={abrirAlta}
                    disabled={sinConexion}
                >
                    Nuevo resguardo
                </Button>
            </Box>

            <DynamicTable
                columnas={columnas}
                filas={tabla.filas}
                cargando={tabla.cargando}
                anchoMinimo={1060}
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
                onRecargar={recargar}
                vacio={{
                    icono: InventoryIcon,
                    titulo: tabla.busqueda || filtroEstado
                        ? 'Sin resultados'
                        : 'No hay resguardos registrados',
                    descripcion: tabla.busqueda || filtroEstado
                        ? 'Prueba con otros términos o cambia el filtro de estado.'
                        : 'Registra aquí los equipos que se entregan en préstamo al personal.',
                    textoAccion: !tabla.busqueda && !filtroEstado ? 'Registrar el primero' : undefined,
                    onAccion: !tabla.busqueda && !filtroEstado ? abrirAlta : undefined,
                }}
            />

            <Dialog
                open={modalAbierto}
                onClose={() => !isSubmitting && setModalAbierto(false)}
                fullWidth
                maxWidth="md"
                fullScreen={esMovil}
                aria-labelledby="titulo-resguardo"
            >
                <DialogTitle id="titulo-resguardo">Nuevo resguardo</DialogTitle>

                <form onSubmit={handleSubmit(guardar)} noValidate>
                    <DialogContent dividers>
                        <Stack spacing={3}>
                            <Box>
                                <Typography variant="overline" color="text.secondary">
                                    Quién recibe el equipo
                                </Typography>
                                <Stack spacing={2.5} sx={{ mt: 1 }}>
                                    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2.5}>
                                        <CampoFormulario
                                            control={control}
                                            nombre="solicitanteNombre"
                                            etiqueta="Nombre completo"
                                            obligatorio
                                            maximo={100}
                                            mayusculas
                                        />
                                        <CampoFormulario
                                            control={control}
                                            nombre="solicitanteNumero"
                                            etiqueta="Número de empleado"
                                            obligatorio
                                            maximo={30}
                                            mayusculas
                                        />
                                    </Stack>
                                    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2.5}>
                                        <CampoFormulario
                                            control={control}
                                            nombre="departamento"
                                            etiqueta="Departamento"
                                            maximo={100}
                                            mayusculas
                                        />
                                        <CampoFormulario
                                            control={control}
                                            nombre="telefono"
                                            etiqueta="Teléfono o extensión"
                                            maximo={50}
                                        />
                                    </Stack>
                                </Stack>
                            </Box>

                            <Divider />

                            <Box>
                                <Typography variant="overline" color="text.secondary">
                                    Equipo entregado
                                </Typography>
                                <Stack spacing={2.5} sx={{ mt: 1 }}>
                                    <CampoFormulario
                                        control={control}
                                        nombre="equipoNombre"
                                        etiqueta="Equipo"
                                        obligatorio
                                        maximo={100}
                                        mayusculas
                                        ayuda="Por ejemplo LAPTOP DELL LATITUDE 5420."
                                    />
                                    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2.5}>
                                        <CampoFormulario
                                            control={control}
                                            nombre="numeroSerie"
                                            etiqueta="Número de serie"
                                            obligatorio
                                            maximo={50}
                                            mayusculas
                                        />
                                        <CampoFormulario
                                            control={control}
                                            nombre="numeroInventario"
                                            etiqueta="Número de inventario"
                                            maximo={50}
                                            mayusculas
                                        />
                                    </Stack>
                                    <CampoFormulario
                                        control={control}
                                        nombre="accesorios"
                                        etiqueta="Accesorios"
                                        maximo={500}
                                        mayusculas
                                        multiline
                                        rows={2}
                                        ayuda="Cargador, maletín, cables… lo que se entrega junto al equipo."
                                    />
                                    <CampoFormulario
                                        control={control}
                                        nombre="condiciones"
                                        etiqueta="Condiciones de entrega"
                                        maximo={500}
                                        mayusculas
                                        ayuda="Estado físico en el que se entrega."
                                    />
                                </Stack>
                            </Box>

                            <Divider />

                            <Box>
                                <Typography variant="overline" color="text.secondary">
                                    Plazo del préstamo
                                </Typography>
                                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2.5} sx={{ mt: 1 }}>
                                    <CampoFormulario
                                        control={control}
                                        nombre="duracionTipo"
                                        etiqueta="Duración"
                                        obligatorio
                                        opciones={OPCIONES_DURACION}
                                    />
                                    {exigePlazo && (
                                        <CampoFormulario
                                            control={control}
                                            nombre="duracionCantidad"
                                            etiqueta="Cantidad"
                                            obligatorio
                                            type="number"
                                            ayuda="Entre 1 y 365."
                                        />
                                    )}
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
                            {isSubmitting ? 'Guardando…' : 'Registrar resguardo'}
                        </Button>
                    </DialogActions>
                </form>
            </Dialog>

            <ConfirmationDialog
                abierto={Boolean(resguardoADevolver)}
                titulo="¿Registrar la devolución?"
                mensaje={`El equipo "${resguardoADevolver?.equipoNombre}" que tiene ${resguardoADevolver?.solicitanteNombre} se marcará como devuelto y el resguardo quedará cerrado.`}
                textoConfirmar="Sí, registrar devolución"
                cargando={procesando}
                onConfirmar={confirmarDevolucion}
                onCancelar={() => setResguardoADevolver(null)}
            />
        </Box>
    );
}
