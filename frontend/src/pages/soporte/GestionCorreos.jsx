import { useState, useEffect, useCallback } from 'react';
import {
    Box, Typography, Button, Chip, Tooltip, Dialog, DialogTitle, DialogContent,
    DialogActions, Stack, useMediaQuery, Divider, TextField, MenuItem
} from '@mui/material';
import { useTheme } from '@mui/material/styles';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import MailIcon from '@mui/icons-material/Mail';
import SwapHorizIcon from '@mui/icons-material/SwapHoriz';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutlineOutlined';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import api from '../../services/api';
import { correoService } from '../../services/correoService';
import { useNotification } from '../../context/NotificationContext.jsx';
import { useNetworkStatus } from '../../hooks/useNetworkStatus.jsx';
import { useTablaPaginada } from '../../hooks/useTablaPaginada.jsx';
import { useRol } from '../../hooks/useRol.jsx';
import { esquemaCorreo } from '../../util/esquemas';
import { truncar } from '../../util/formater';

import DynamicTable from '../../components/DynamicTable';
import AccionesTabla from '../../components/AccionesTabla';
import ConfirmationDialog from '../../components/ConfirmationDialog';
import CampoFormulario from '../../components/CampoFormulario';

const VALORES_INICIALES = {
    nombre: '',
    apellidoPaterno: '',
    apellidoMaterno: '',
    correo: '',
    area: '',
    cargo: '',
    extension: '',
    cuotaAlmacenamiento: '5 GB',
};

/** Color del distintivo según el estado de la cuenta. */
const colorEstado = (estado) => {
    switch ((estado || '').toUpperCase()) {
        case 'ACTIVO': return 'success';
        case 'SUSPENDIDO': return 'warning';
        case 'BAJA': return 'default';
        default: return 'default';
    }
};

/**
 * Directorio de correos institucionales.
 *
 * Cambios respecto a la versión anterior:
 *  - El formulario incluía el campo `estado` y lo enviaba en la edición. El
 *    backend lo copiaba tal cual, saltándose el endpoint de cambio de estado
 *    —el único que valida contra el enum—, de modo que una cuenta podía quedar
 *    en un estado inexistente e invisible para los filtros. Ahora el estado
 *    solo se cambia desde su propia acción.
 *  - `correoService` envolvía los errores en `new Error(...)` leyendo un campo
 *    que el backend no envía, así que todos los fallos mostraban el mismo
 *    texto genérico.
 *  - El listado traía el directorio completo sin paginar.
 *  - "Eliminar" borraba de forma definitiva tras un `window.confirm()` que no
 *    distinguía entre dar de baja y borrar el historial.
 *  - Montaba su propio Snackbar en lugar de usar el del sistema.
 */
export default function GestionCorreos() {
    const { notificar, notificarError, notificarInfo } = useNotification();
    const { esAdministrador, nombre: nombreTecnico } = useRol();

    const theme = useTheme();
    const esMovil = useMediaQuery(theme.breakpoints.down('sm'));

    const estadoRed = useNetworkStatus();
    const sinConexion = Boolean(estadoRed.sinConexion ?? estadoRed);

    const [filtroEstado, setFiltroEstado] = useState('');
    const [estados, setEstados] = useState([]);

    const [correoEditando, setCorreoEditando] = useState(null);
    const [modalAbierto, setModalAbierto] = useState(false);

    const [correoCambioEstado, setCorreoCambioEstado] = useState(null);
    const [nuevoEstado, setNuevoEstado] = useState('');
    const [correoABorrar, setCorreoABorrar] = useState(null);
    const [procesando, setProcesando] = useState(false);

    const cargar = useCallback((params) => correoService.getAll(params), []);

    const tabla = useTablaPaginada({
        cargar,
        ordenInicial: { campo: 'nombre', direccion: 'asc' },
        filtros: { estado: filtroEstado || undefined },
    });

    const { recargar } = tabla;

    const {
        control,
        handleSubmit,
        reset,
        formState: { isSubmitting },
    } = useForm({
        resolver: zodResolver(esquemaCorreo),
        mode: 'onBlur',
        defaultValues: VALORES_INICIALES,
    });

    // --- Catálogo de estados ----------------------------------------------
    useEffect(() => {
        let cancelado = false;

        correoService.getEstados()
            .then((respuesta) => {
                if (!cancelado) setEstados(respuesta?.data ?? []);
            })
            .catch(() => {
                if (!cancelado) setEstados([]);
            });

        return () => { cancelado = true; };
    }, []);

    const abrirAlta = () => {
        setCorreoEditando(null);
        reset(VALORES_INICIALES);
        setModalAbierto(true);
    };

    const abrirEdicion = (correo) => {
        setCorreoEditando(correo);
        reset({
            nombre: correo.nombre || '',
            apellidoPaterno: correo.apellidoPaterno || '',
            apellidoMaterno: correo.apellidoMaterno || '',
            correo: correo.correo || '',
            area: correo.area || '',
            cargo: correo.cargo || '',
            extension: correo.extension || '',
            cuotaAlmacenamiento: correo.cuotaAlmacenamiento || '',
        });
        setModalAbierto(true);
    };

    const guardar = async (datos) => {
        const carga = {
            nombre: datos.nombre.trim(),
            apellidoPaterno: datos.apellidoPaterno.trim(),
            apellidoMaterno: datos.apellidoMaterno?.trim() || null,
            correo: datos.correo.trim().toLowerCase(),
            area: datos.area.trim(),
            cargo: datos.cargo?.trim() || null,
            extension: datos.extension?.trim() || null,
            cuotaAlmacenamiento: datos.cuotaAlmacenamiento?.trim() || null,
        };

        try {
            const respuesta = correoEditando
                ? await correoService.update(correoEditando.id, carga)
                : await correoService.create(carga);

            notificar(respuesta?.mensaje
                || (correoEditando ? 'Correo actualizado correctamente.' : 'Correo institucional registrado.'));
            setModalAbierto(false);
            recargar();
        } catch (error) {
            notificarError(error);
        }
    };

    const abrirCambioEstado = (correo) => {
        setCorreoCambioEstado(correo);
        setNuevoEstado(correo.estado || '');
    };

    const confirmarCambioEstado = async () => {
        setProcesando(true);
        try {
            const respuesta = await correoService.cambiarEstado(correoCambioEstado.id, nuevoEstado);
            notificar(respuesta?.mensaje || 'Estado actualizado.');
            setCorreoCambioEstado(null);
            recargar();
        } catch (error) {
            notificarError(error);
        } finally {
            setProcesando(false);
        }
    };

    const confirmarBorrado = async () => {
        setProcesando(true);
        try {
            const respuesta = await correoService.delete(correoABorrar.id);
            notificar(respuesta?.mensaje || 'Registro eliminado correctamente.');
            setCorreoABorrar(null);
            recargar();
        } catch (error) {
            notificarError(error);
        } finally {
            setProcesando(false);
        }
    };

    /** Genera el dictamen de la gestión de la cuenta, para el expediente. */
    const generarDictamen = async (correo) => {
        notificarInfo('Generando el documento…');

        const nombreCompleto = [correo.nombre, correo.apellidoPaterno, correo.apellidoMaterno]
            .filter(Boolean)
            .join(' ');

        try {
            const respuesta = await api.post('/v1/documentos/dictamen', {
                folioTicket: correo.id ?? 0,
                fecha: new Date().toLocaleDateString('es-MX'),
                direccionUsuario: 'N/A',
                departamentoUsuario: correo.area || 'N/A',
                nombreUsuario: `C. ${nombreCompleto}`,
                telefonoUsuario: correo.extension || 'N/A',
                tipoReporte: 'GESTIÓN Y ASIGNACIÓN DE CORREO INSTITUCIONAL',
                cve: 'N/A',
                descripcionEquipo: `CUENTA DE CORREO: ${correo.correo}`,
                marca: `CUOTA: ${correo.cuotaAlmacenamiento || 'N/A'}`,
                modelo: 'N/A',
                serie: 'N/A',
                noResguardo: 'N/A',
                fallaReportada: `SOLICITUD PARA CUENTA DE CORREO INSTITUCIONAL. ESTADO: ${correo.estado}`,
                diagnostico: 'VERIFICACIÓN Y CONFIGURACIÓN DE PARÁMETROS EN EL SERVIDOR DE CORREOS.',
                hallazgos: `CARGO: ${correo.cargo || 'N/A'}`,
                conclusion: 'SE PROCESÓ LA SOLICITUD DE LA CUENTA DE CORREO INSTITUCIONAL DE MANERA EXITOSA.',
                // El nombre de quien firma sale de la sesión, no de un valor
                // escrito a mano: antes el revisor estaba fijo en el código.
                realizadoPor: `C. ${(nombreTecnico || 'SOPORTE TÉCNICO').toUpperCase()}`,
            }, { responseType: 'blob' });

            const url = window.URL.createObjectURL(
                new Blob([respuesta.data], { type: 'application/pdf' })
            );
            const ventana = window.open(url, '_blank');

            if (!ventana) {
                notificarError('El navegador bloqueó la ventana. Permite las ventanas emergentes para ver el documento.');
            }

            setTimeout(() => window.URL.revokeObjectURL(url), 60000);
        } catch (error) {
            notificarError(error);
        }
    };

    const etiquetaDe = (valor) =>
        estados.find((e) => e.valor === valor)?.etiqueta || valor || '—';

    // --- Columnas ---------------------------------------------------------
    const columnas = [
        {
            id: 'nombre',
            etiqueta: 'Titular',
            principal: true,
            ordenable: true,
            render: (c) => (
                <Box>
                    <Typography variant="body2" fontWeight={500}>
                        {truncar([c.nombre, c.apellidoPaterno, c.apellidoMaterno]
                            .filter(Boolean).join(' '), 34)}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                        {c.cargo || 'Sin cargo registrado'}
                    </Typography>
                </Box>
            ),
        },
        {
            id: 'correo',
            etiqueta: 'Cuenta',
            ancho: '24%',
            ordenable: true,
            render: (c) => (
                <Tooltip title={c.correo || ''} arrow>
                    <Typography variant="body2" color="text.secondary">
                        {truncar(c.correo, 32)}
                    </Typography>
                </Tooltip>
            ),
        },
        { id: 'area', etiqueta: 'Área', ancho: '18%', ordenable: true },
        {
            id: 'extension',
            etiqueta: 'Extensión',
            ancho: 110,
            sinOrden: true,
            render: (c) => (
                <Typography variant="body2" color="text.secondary">
                    {c.extension || '—'}
                </Typography>
            ),
        },
        {
            id: 'cuotaAlmacenamiento',
            etiqueta: 'Cuota',
            ancho: 100,
            sinOrden: true,
            render: (c) => (
                <Typography variant="body2" color="text.secondary">
                    {c.cuotaAlmacenamiento || '—'}
                </Typography>
            ),
        },
        {
            id: 'estado',
            etiqueta: 'Estado',
            alineacion: 'center',
            ancho: 130,
            render: (c) => (
                <Chip
                    label={etiquetaDe(c.estado)}
                    size="small"
                    color={colorEstado(c.estado)}
                    variant={c.estado === 'BAJA' ? 'outlined' : 'filled'}
                    sx={{ minWidth: 96 }}
                />
            ),
        },
        {
            id: 'acciones',
            etiqueta: 'Acciones',
            alineacion: 'center',
            ancho: 160,
            sinOrden: true,
            render: (c) => (
                <AccionesTabla
                    acciones={[
                        {
                            id: 'editar',
                            icono: <EditIcon />,
                            titulo: 'Editar',
                            etiqueta: `Editar los datos de ${c.correo}`,
                            onClick: () => abrirEdicion(c),
                            deshabilitada: sinConexion,
                            motivoDeshabilitada: 'Sin conexión con el servidor',
                        },
                        {
                            id: 'estado',
                            icono: <SwapHorizIcon />,
                            titulo: 'Cambiar estado',
                            etiqueta: `Cambiar el estado de la cuenta ${c.correo}`,
                            color: 'primary',
                            onClick: () => abrirCambioEstado(c),
                            deshabilitada: sinConexion,
                            motivoDeshabilitada: 'Sin conexión con el servidor',
                        },
                        {
                            id: 'dictamen',
                            icono: <PictureAsPdfIcon />,
                            titulo: 'Generar dictamen',
                            etiqueta: `Generar el dictamen de la cuenta ${c.correo}`,
                            onClick: () => generarDictamen(c),
                            deshabilitada: sinConexion,
                            motivoDeshabilitada: 'Sin conexión con el servidor',
                        },
                        {
                            id: 'borrar',
                            icono: <DeleteOutlineIcon />,
                            titulo: 'Eliminar registro',
                            etiqueta: `Eliminar definitivamente el registro de ${c.correo}`,
                            color: 'error',
                            // El borrado definitivo elimina la trazabilidad de
                            // la cuenta; el backend lo restringe a administración.
                            oculta: !esAdministrador,
                            onClick: () => setCorreoABorrar(c),
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
                        <MailIcon fontSize="large" aria-hidden="true" />
                        Correos institucionales
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        Altas, bajas y suspensiones de cuentas del personal.
                    </Typography>
                </Box>

                <Button
                    variant="contained"
                    startIcon={<AddIcon />}
                    onClick={abrirAlta}
                    disabled={sinConexion}
                >
                    Nueva cuenta
                </Button>
            </Box>

            <DynamicTable
                columnas={columnas}
                filas={tabla.filas}
                cargando={tabla.cargando}
                anchoMinimo={1180}
                busqueda={tabla.busqueda}
                onBuscar={tabla.setBusqueda}
                placeholderBusqueda="Buscar por titular, cuenta, área o cargo…"
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
                    icono: MailIcon,
                    titulo: tabla.busqueda || filtroEstado
                        ? 'Sin resultados'
                        : 'No hay cuentas registradas',
                    descripcion: tabla.busqueda || filtroEstado
                        ? 'Prueba con otros términos o cambia el filtro de estado.'
                        : 'Registra aquí las cuentas de correo institucional del personal.',
                    textoAccion: !tabla.busqueda && !filtroEstado ? 'Registrar la primera' : undefined,
                    onAccion: !tabla.busqueda && !filtroEstado ? abrirAlta : undefined,
                }}
            />

            {/* ------------------------------------------ alta y edición ---- */}
            <Dialog
                open={modalAbierto}
                onClose={() => !isSubmitting && setModalAbierto(false)}
                fullWidth
                maxWidth="md"
                fullScreen={esMovil}
                aria-labelledby="titulo-correo"
            >
                <DialogTitle id="titulo-correo">
                    {correoEditando ? 'Editar cuenta' : 'Nueva cuenta de correo'}
                </DialogTitle>

                <form onSubmit={handleSubmit(guardar)} noValidate>
                    <DialogContent dividers>
                        <Stack spacing={3}>
                            <Box>
                                <Typography variant="overline" color="text.secondary">
                                    Titular de la cuenta
                                </Typography>
                                <Stack spacing={2.5} sx={{ mt: 1 }}>
                                    <CampoFormulario
                                        control={control}
                                        nombre="nombre"
                                        etiqueta="Nombre(s)"
                                        obligatorio
                                        maximo={100}
                                        mayusculas
                                    />
                                    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2.5}>
                                        <CampoFormulario
                                            control={control}
                                            nombre="apellidoPaterno"
                                            etiqueta="Apellido paterno"
                                            obligatorio
                                            maximo={100}
                                            mayusculas
                                        />
                                        <CampoFormulario
                                            control={control}
                                            nombre="apellidoMaterno"
                                            etiqueta="Apellido materno"
                                            maximo={100}
                                            mayusculas
                                        />
                                    </Stack>
                                </Stack>
                            </Box>

                            <Divider />

                            <Box>
                                <Typography variant="overline" color="text.secondary">
                                    Datos laborales
                                </Typography>
                                <Stack spacing={2.5} sx={{ mt: 1 }}>
                                    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2.5}>
                                        <CampoFormulario
                                            control={control}
                                            nombre="area"
                                            etiqueta="Área"
                                            obligatorio
                                            maximo={150}
                                            mayusculas
                                        />
                                        <CampoFormulario
                                            control={control}
                                            nombre="cargo"
                                            etiqueta="Cargo"
                                            maximo={150}
                                            mayusculas
                                        />
                                    </Stack>
                                    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2.5}>
                                        <CampoFormulario
                                            control={control}
                                            nombre="extension"
                                            etiqueta="Extensión"
                                            maximo={20}
                                        />
                                        <CampoFormulario
                                            control={control}
                                            nombre="cuotaAlmacenamiento"
                                            etiqueta="Cuota de almacenamiento"
                                            maximo={30}
                                            ayuda="Por ejemplo 5 GB."
                                        />
                                    </Stack>
                                </Stack>
                            </Box>

                            <Divider />

                            <Box>
                                <Typography variant="overline" color="text.secondary">
                                    Cuenta
                                </Typography>
                                <Box sx={{ mt: 1 }}>
                                    <CampoFormulario
                                        control={control}
                                        nombre="correo"
                                        etiqueta="Correo institucional"
                                        obligatorio
                                        maximo={150}
                                        type="email"
                                        ayuda={correoEditando
                                            ? 'El estado de la cuenta se cambia desde su propia acción en la tabla.'
                                            : 'La cuenta se registra activa. Después podrás suspenderla o darla de baja.'}
                                    />
                                </Box>
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
                                : correoEditando ? 'Guardar cambios' : 'Registrar cuenta'}
                        </Button>
                    </DialogActions>
                </form>
            </Dialog>

            {/* -------------------------------------- cambio de estado ----- */}
            <Dialog
                open={Boolean(correoCambioEstado)}
                onClose={() => !procesando && setCorreoCambioEstado(null)}
                fullWidth
                maxWidth="xs"
                fullScreen={esMovil}
                aria-labelledby="titulo-estado-correo"
            >
                <DialogTitle id="titulo-estado-correo">Estado de la cuenta</DialogTitle>

                <DialogContent dividers>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 2.5 }}>
                        Cuenta <strong>{correoCambioEstado?.correo}</strong>. Dar de baja
                        conserva el registro y su historial; para eliminarlo por completo
                        existe la acción de borrado.
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
                        onClick={() => setCorreoCambioEstado(null)}
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
                            || nuevoEstado === correoCambioEstado?.estado}
                        fullWidth={esMovil}
                        size={esMovil ? 'large' : 'medium'}
                    >
                        {procesando ? 'Guardando…' : 'Cambiar estado'}
                    </Button>
                </DialogActions>
            </Dialog>

            <ConfirmationDialog
                abierto={Boolean(correoABorrar)}
                titulo="¿Eliminar este registro?"
                mensaje={`El registro de "${correoABorrar?.correo}" se borrará de forma permanente, junto con su trazabilidad. Si solo quieres cerrar la cuenta, cambia su estado a Baja.`}
                textoConfirmar="Sí, eliminar"
                destructivo
                cargando={procesando}
                onConfirmar={confirmarBorrado}
                onCancelar={() => setCorreoABorrar(null)}
            />
        </Box>
    );
}
