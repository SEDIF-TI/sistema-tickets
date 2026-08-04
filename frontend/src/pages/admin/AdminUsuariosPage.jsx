import { useState, useEffect, useCallback, useMemo } from 'react';
import {
    Box, Typography, Button, Chip, Switch, Tooltip, Dialog, DialogTitle,
    DialogContent, DialogActions, Stack, useMediaQuery, Alert
} from '@mui/material';
import { useTheme } from '@mui/material/styles';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import GroupIcon from '@mui/icons-material/Group';
import BlockIcon from '@mui/icons-material/Block';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutlined';
import VpnKeyIcon from '@mui/icons-material/VpnKey';
import { useForm, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import { userService, rolService } from '../../services/userService';
import { areaService } from '../../services/areaService';
import { useNotification } from '../../context/NotificationContext.jsx';
import { useNetworkStatus } from '../../hooks/useNetworkStatus.jsx';
import { useTablaPaginada } from '../../hooks/useTablaPaginada.jsx';
import { useRol } from '../../hooks/useRol.jsx';
import { esquemaUsuario } from '../../util/esquemas';
import { truncar } from '../../util/formater';

import DynamicTable from '../../components/DynamicTable';
import AccionesTabla from '../../components/AccionesTabla';
import ConfirmationDialog from '../../components/ConfirmationDialog';
import CampoFormulario from '../../components/CampoFormulario';
import ModalCredenciales from '../../components/ModalCredenciales';

/** Filtro de estado. El backend espera un booleano, de ahí las cadenas. */
const OPCIONES_ESTADO = [
    { valor: '', etiqueta: 'Todos los estados' },
    { valor: 'true', etiqueta: 'Activos' },
    { valor: 'false', etiqueta: 'Inactivos' },
];

const VALORES_INICIALES = {
    nombre: '',
    apellidoPaterno: '',
    apellidoMaterno: '',
    correo: '',
    username: '',
    rolId: '',
    areaId: '',
};

/**
 * Administración de usuarios.
 *
 * Cambios respecto a la versión anterior:
 *  - Los roles estaban escritos a mano con los ids 4, 5 y 6, que no son los de
 *    la base de datos (1, 2 y 3): crear un usuario fallaba siempre. Ahora el
 *    catálogo se pide al backend.
 *  - Los apellidos se capturaban pero el DTO del backend no los aceptaba, así
 *    que se descartaban en silencio.
 *  - La tabla traía todos los usuarios y filtraba en el navegador; ahora la
 *    paginación, la búsqueda y los filtros los resuelve la base de datos.
 *  - Usaba `alert()` y `window.confirm()` nativos, bloqueantes y sin estilo.
 *  - "Reset Clave" se ejecutaba sin confirmar y descartaba la contraseña
 *    generada: el administrador no llegaba a verla nunca.
 *  - Las tres acciones eran botones de texto que ensanchaban la tabla.
 */
export default function AdminUsuariosPage() {
    const { notificar, notificarError } = useNotification();
    const { usuarioId } = useRol();

    const theme = useTheme();
    const esMovil = useMediaQuery(theme.breakpoints.down('sm'));

    const estadoRed = useNetworkStatus();
    const sinConexion = Boolean(estadoRed.sinConexion ?? estadoRed);

    const [filtroRol, setFiltroRol] = useState('');
    const [filtroEstado, setFiltroEstado] = useState('');

    const [roles, setRoles] = useState([]);
    const [areas, setAreas] = useState([]);

    const [usuarioEditando, setUsuarioEditando] = useState(null);
    const [modalAbierto, setModalAbierto] = useState(false);

    const [usuarioABaja, setUsuarioABaja] = useState(null);
    const [usuarioAResetear, setUsuarioAResetear] = useState(null);
    const [procesando, setProcesando] = useState(false);

    const [credenciales, setCredenciales] = useState(null);

    const cargar = useCallback((params) => userService.getAll(params), []);

    const tabla = useTablaPaginada({
        cargar,
        ordenInicial: { campo: 'nombre', direccion: 'asc' },
        filtros: {
            rol: filtroRol || undefined,
            activo: filtroEstado || undefined,
        },
    });

    const { recargar } = tabla;

    // Mapa id -> nombre del rol, que el esquema necesita para saber si el área
    // es obligatoria (el administrador no pertenece a un área concreta).
    const rolesPorId = useMemo(
        () => Object.fromEntries(roles.map((r) => [String(r.id), r.nombre])),
        [roles]
    );

    const esquema = useMemo(() => esquemaUsuario(rolesPorId), [rolesPorId]);

    const {
        control,
        handleSubmit,
        reset,
        formState: { isSubmitting },
    } = useForm({
        resolver: zodResolver(esquema),
        mode: 'onBlur',
        defaultValues: VALORES_INICIALES,
    });

    // useWatch en lugar de watch(): se suscribe solo a este campo, sin
    // repintar el formulario entero en cada tecla de los demas.
    const rolIdElegido = useWatch({ control, name: 'rolId' });
    const rolElegido = rolesPorId[rolIdElegido];

    // --- Catálogos --------------------------------------------------------
    useEffect(() => {
        let cancelado = false;

        const cargarCatalogos = async () => {
            try {
                const [resRoles, resAreas] = await Promise.all([
                    rolService.getAll(),
                    areaService.getAll(),
                ]);

                if (cancelado) return;

                setRoles(resRoles?.data ?? []);

                const listaAreas = Array.isArray(resAreas?.data) ? resAreas.data : [];
                setAreas([...listaAreas].sort((a, b) => a.nombre.localeCompare(b.nombre, 'es')));
            } catch (error) {
                if (!cancelado) notificarError(error);
            }
        };

        cargarCatalogos();
        return () => { cancelado = true; };
    }, [notificarError]);

    // --- Modal ------------------------------------------------------------
    const abrirAlta = () => {
        setUsuarioEditando(null);
        reset(VALORES_INICIALES);
        setModalAbierto(true);
    };

    const abrirEdicion = (usuario) => {
        setUsuarioEditando(usuario);
        reset({
            nombre: usuario.nombre || '',
            apellidoPaterno: usuario.apellidoPaterno || '',
            apellidoMaterno: usuario.apellidoMaterno || '',
            correo: usuario.correo || '',
            username: usuario.username || '',
            rolId: usuario.rolId ? String(usuario.rolId) : '',
            areaId: usuario.areaId ? String(usuario.areaId) : '',
        });
        setModalAbierto(true);
    };

    const cerrarModal = () => {
        if (isSubmitting) return;   // no se cierra a media operación
        setModalAbierto(false);
    };

    const guardar = async (datos) => {
        const carga = {
            nombre: datos.nombre.trim(),
            apellidoPaterno: datos.apellidoPaterno.trim(),
            apellidoMaterno: datos.apellidoMaterno?.trim() || null,
            correo: datos.correo.trim().toLowerCase(),
            rolId: Number(datos.rolId),
            areaId: datos.areaId ? Number(datos.areaId) : null,
        };

        try {
            if (usuarioEditando) {
                const respuesta = await userService.update(usuarioEditando.id, carga);
                notificar(respuesta?.mensaje || 'Usuario actualizado correctamente.');
            } else {
                const respuesta = await userService.create({
                    ...carga,
                    // El nombre de usuario solo se fija al crear: después es la
                    // identidad con la que la persona entra.
                    username: datos.username?.trim() || null,
                    disponibleSoporte: false,
                });

                const creado = respuesta?.data;

                // La contraseña temporal solo viaja en esta respuesta: si no se
                // muestra ahora, se pierde y hay que restablecerla.
                setCredenciales({
                    nombre: creado?.nombreCompleto || carga.nombre,
                    correo: creado?.correo,
                    rol: creado?.rolNombre,
                    area: creado?.areaNombre,
                    password: creado?.passwordTemporalTexto,
                });
            }

            setModalAbierto(false);
            recargar();
        } catch (error) {
            notificarError(error);
        }
    };

    // --- Acciones de fila -------------------------------------------------
    const confirmarBaja = async () => {
        setProcesando(true);
        try {
            const respuesta = await userService.toggleEstado(usuarioABaja.id);
            notificar(respuesta?.mensaje || 'Estado del usuario actualizado.');
            setUsuarioABaja(null);
            recargar();
        } catch (error) {
            notificarError(error);
        } finally {
            setProcesando(false);
        }
    };

    const confirmarReset = async () => {
        setProcesando(true);
        try {
            const respuesta = await userService.resetPassword(usuarioAResetear.id);
            const datos = respuesta?.data;

            // La versión anterior descartaba la respuesta, así que la clave
            // recién generada no se mostraba en ninguna parte.
            setCredenciales({
                nombre: datos?.nombreCompleto || usuarioAResetear.nombreCompleto,
                correo: datos?.correo,
                rol: datos?.rolNombre,
                area: datos?.areaNombre,
                password: datos?.passwordTemporalTexto,
            });

            setUsuarioAResetear(null);
            recargar();
        } catch (error) {
            notificarError(error);
        } finally {
            setProcesando(false);
        }
    };

    const cambiarDisponibilidad = async (usuario, disponible) => {
        try {
            await userService.actualizarDisponibilidad(usuario.id, disponible);
            notificar(
                disponible
                    ? `${usuario.nombre} vuelve a recibir tickets automáticamente.`
                    : `${usuario.nombre} deja de recibir tickets automáticamente.`
            );
            recargar();
        } catch (error) {
            notificarError(error);
        }
    };

    // --- Columnas ---------------------------------------------------------
    const columnas = [
        {
            id: 'nombre',
            etiqueta: 'Nombre',
            principal: true,
            ordenable: true,
            render: (u) => (
                <Box>
                    <Typography variant="body2" fontWeight={500}>
                        {truncar(u.nombreCompleto || u.nombre, 42)}
                    </Typography>
                    {u.username && (
                        <Typography variant="caption" color="text.secondary">
                            {u.username}
                        </Typography>
                    )}
                </Box>
            ),
        },
        {
            id: 'correo',
            etiqueta: 'Correo',
            ancho: '22%',
            ordenable: true,
            render: (u) => (
                <Tooltip title={u.correo || ''} arrow>
                    <Typography variant="body2" color="text.secondary">
                        {truncar(u.correo, 30)}
                    </Typography>
                </Tooltip>
            ),
        },
        { id: 'rolNombre', etiqueta: 'Rol', ancho: 140 },
        { id: 'areaNombre', etiqueta: 'Área', ancho: '16%' },
        {
            id: 'disponibleSoporte',
            etiqueta: 'Recibe tickets',
            alineacion: 'center',
            ancho: 130,
            sinOrden: true,
            render: (u) => {
                // Solo tiene sentido para quien atiende tickets.
                if (u.rolNombre !== 'SOPORTE') {
                    return <Typography variant="caption" color="text.secondary">—</Typography>;
                }

                return (
                    <Tooltip
                        title={u.disponibleSoporte
                            ? 'El balanceador le asigna tickets nuevos'
                            : 'El balanceador lo omite'}
                        arrow
                    >
                        <span>
                            <Switch
                                checked={Boolean(u.disponibleSoporte)}
                                onChange={(e) => cambiarDisponibilidad(u, e.target.checked)}
                                disabled={!u.activo || sinConexion}
                                color="success"
                                inputProps={{
                                    'aria-label': `Recepción automática de tickets de ${u.nombre}`,
                                }}
                            />
                        </span>
                    </Tooltip>
                );
            },
        },
        {
            id: 'activo',
            etiqueta: 'Estado',
            alineacion: 'center',
            ancho: 150,
            render: (u) => (
                <Stack direction="row" spacing={0.5} justifyContent="center" flexWrap="wrap" useFlexGap>
                    <Chip
                        label={u.activo ? 'Activo' : 'Inactivo'}
                        size="small"
                        color={u.activo ? 'success' : 'default'}
                        variant={u.activo ? 'filled' : 'outlined'}
                    />
                    {u.passwordTemporal && (
                        <Tooltip title="Debe cambiar la contraseña al entrar" arrow>
                            <Chip label="Clave temporal" size="small" color="warning" variant="outlined" />
                        </Tooltip>
                    )}
                </Stack>
            ),
        },
        {
            id: 'acciones',
            etiqueta: 'Acciones',
            alineacion: 'center',
            ancho: 140,
            sinOrden: true,
            render: (u) => {
                // Nadie se da de baja a sí mismo: se quedaría fuera del sistema
                // sin poder revertirlo.
                const esMiCuenta = u.id === usuarioId;

                return (
                    <AccionesTabla
                        acciones={[
                            {
                                id: 'editar',
                                icono: <EditIcon />,
                                titulo: 'Editar',
                                etiqueta: `Editar los datos de ${u.nombre}`,
                                onClick: () => abrirEdicion(u),
                                deshabilitada: sinConexion,
                                motivoDeshabilitada: 'Sin conexión con el servidor',
                            },
                            {
                                id: 'clave',
                                icono: <VpnKeyIcon />,
                                titulo: 'Restablecer contraseña',
                                etiqueta: `Restablecer la contraseña de ${u.nombre}`,
                                onClick: () => setUsuarioAResetear(u),
                                deshabilitada: sinConexion || !u.activo,
                                motivoDeshabilitada: !u.activo
                                    ? 'El usuario está dado de baja'
                                    : 'Sin conexión con el servidor',
                            },
                            {
                                id: 'estado',
                                icono: u.activo ? <BlockIcon /> : <CheckCircleOutlineIcon />,
                                titulo: u.activo ? 'Dar de baja' : 'Reactivar',
                                etiqueta: `${u.activo ? 'Dar de baja a' : 'Reactivar a'} ${u.nombre}`,
                                color: u.activo ? 'error' : 'success',
                                onClick: () => setUsuarioABaja(u),
                                deshabilitada: sinConexion || esMiCuenta,
                                motivoDeshabilitada: esMiCuenta
                                    ? 'No puedes dar de baja tu propia cuenta'
                                    : 'Sin conexión con el servidor',
                            },
                        ]}
                    />
                );
            },
        },
    ];

    const opcionesRol = [
        { valor: '', etiqueta: 'Todos los roles' },
        ...roles.map((r) => ({ valor: r.nombre, etiqueta: r.nombre })),
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
                        <GroupIcon fontSize="large" aria-hidden="true" />
                        Usuarios
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        Altas, bajas y permisos del personal del sistema.
                    </Typography>
                </Box>

                <Button
                    variant="contained"
                    startIcon={<AddIcon />}
                    onClick={abrirAlta}
                    disabled={sinConexion || roles.length === 0}
                >
                    Nuevo usuario
                </Button>
            </Box>

            <DynamicTable
                columnas={columnas}
                filas={tabla.filas}
                cargando={tabla.cargando}
                anchoMinimo={1100}
                busqueda={tabla.busqueda}
                onBuscar={tabla.setBusqueda}
                placeholderBusqueda="Buscar por nombre, correo o usuario…"
                filtros={[
                    {
                        id: 'rol',
                        etiqueta: 'Rol',
                        valor: filtroRol,
                        valorPorDefecto: '',
                        opciones: opcionesRol,
                        onChange: setFiltroRol,
                        ancho: 190,
                    },
                    {
                        id: 'estado',
                        etiqueta: 'Estado',
                        valor: filtroEstado,
                        valorPorDefecto: '',
                        opciones: OPCIONES_ESTADO,
                        onChange: setFiltroEstado,
                        ancho: 170,
                    },
                ]}
                paginacion={tabla.paginacion}
                onCambiarPagina={tabla.cambiarPagina}
                onCambiarTamano={tabla.cambiarTamano}
                orden={tabla.orden}
                onCambiarOrden={tabla.cambiarOrden}
                onRecargar={recargar}
                vacio={{
                    icono: GroupIcon,
                    titulo: tabla.busqueda || filtroRol || filtroEstado
                        ? 'Sin resultados'
                        : 'No hay usuarios registrados',
                    descripcion: tabla.busqueda || filtroRol || filtroEstado
                        ? 'Prueba con otros términos de búsqueda o cambia los filtros.'
                        : 'Da de alta al personal que usará el sistema.',
                    textoAccion: !tabla.busqueda && !filtroRol && !filtroEstado
                        ? 'Dar de alta al primer usuario'
                        : undefined,
                    onAccion: !tabla.busqueda && !filtroRol && !filtroEstado ? abrirAlta : undefined,
                }}
            />

            {/* ------------------------------------------ alta y edición ---- */}
            <Dialog
                open={modalAbierto}
                onClose={cerrarModal}
                fullWidth
                maxWidth="sm"
                fullScreen={esMovil}
                aria-labelledby="titulo-usuario"
            >
                <DialogTitle id="titulo-usuario">
                    {usuarioEditando ? 'Editar usuario' : 'Nuevo usuario'}
                </DialogTitle>

                <form onSubmit={handleSubmit(guardar)} noValidate>
                    <DialogContent dividers>
                        {!usuarioEditando && (
                            <Alert severity="info" sx={{ mb: 3 }}>
                                Al guardar se genera una contraseña temporal. Se mostrará
                                una sola vez, así que anótala o imprímela antes de cerrar.
                            </Alert>
                        )}

                        <Stack spacing={2.5}>
                            <CampoFormulario
                                control={control}
                                nombre="nombre"
                                etiqueta="Nombre(s)"
                                obligatorio
                                maximo={150}
                                mayusculas
                            />

                            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2.5}>
                                <CampoFormulario
                                    control={control}
                                    nombre="apellidoPaterno"
                                    etiqueta="Apellido paterno"
                                    obligatorio
                                    maximo={255}
                                    mayusculas
                                />
                                <CampoFormulario
                                    control={control}
                                    nombre="apellidoMaterno"
                                    etiqueta="Apellido materno"
                                    maximo={255}
                                    mayusculas
                                />
                            </Stack>

                            <CampoFormulario
                                control={control}
                                nombre="correo"
                                etiqueta="Correo electrónico"
                                obligatorio
                                maximo={100}
                                type="email"
                                ayuda="Con este correo entra al sistema."
                            />

                            <CampoFormulario
                                control={control}
                                nombre="username"
                                etiqueta="Nombre de usuario"
                                maximo={50}
                                // Cambiarlo dejaría fuera a quien ya lo usa para entrar.
                                disabled={Boolean(usuarioEditando)}
                                ayuda={usuarioEditando
                                    ? 'No se puede cambiar: es la identidad con la que entra.'
                                    : 'Opcional. Permite entrar sin escribir el correo completo.'}
                            />

                            <CampoFormulario
                                control={control}
                                nombre="rolId"
                                etiqueta="Rol"
                                obligatorio
                                opciones={roles.map((r) => ({
                                    valor: String(r.id),
                                    etiqueta: r.nombre,
                                }))}
                                ayuda="Determina qué pantallas ve y qué puede hacer."
                            />

                            <CampoFormulario
                                control={control}
                                nombre="areaId"
                                etiqueta="Área"
                                obligatorio={rolElegido !== 'ADMINISTRADOR'}
                                opciones={areas.map((a) => ({
                                    valor: String(a.id),
                                    etiqueta: a.nombre,
                                }))}
                                ayuda={rolElegido === 'ADMINISTRADOR'
                                    ? 'Opcional: el administrador ve todas las áreas.'
                                    : 'Determina qué tickets puede consultar.'}
                            />
                        </Stack>
                    </DialogContent>

                    <DialogActions
                        sx={{ flexDirection: { xs: 'column-reverse', sm: 'row' }, gap: 1, p: 2 }}
                    >
                        <Button
                            onClick={cerrarModal}
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
                                : usuarioEditando ? 'Guardar cambios' : 'Crear usuario'}
                        </Button>
                    </DialogActions>
                </form>
            </Dialog>

            {/* ------------------------------------------------ baja ------- */}
            <ConfirmationDialog
                abierto={Boolean(usuarioABaja)}
                titulo={usuarioABaja?.activo ? '¿Dar de baja a este usuario?' : '¿Reactivar a este usuario?'}
                mensaje={usuarioABaja?.activo
                    ? `${usuarioABaja?.nombreCompleto || usuarioABaja?.nombre} dejará de poder entrar al sistema. Su historial de tickets se conserva y puedes reactivarlo cuando quieras.`
                    : `${usuarioABaja?.nombreCompleto || usuarioABaja?.nombre} volverá a tener acceso al sistema con sus permisos anteriores.`}
                textoConfirmar={usuarioABaja?.activo ? 'Sí, dar de baja' : 'Sí, reactivar'}
                destructivo={usuarioABaja?.activo}
                cargando={procesando}
                onConfirmar={confirmarBaja}
                onCancelar={() => setUsuarioABaja(null)}
            />

            {/* ---------------------------------- restablecer contraseña --- */}
            <ConfirmationDialog
                abierto={Boolean(usuarioAResetear)}
                titulo="¿Restablecer la contraseña?"
                mensaje={`Se generará una contraseña temporal para ${usuarioAResetear?.nombreCompleto || usuarioAResetear?.nombre}, que deberá cambiarla al entrar. La contraseña actual dejará de funcionar de inmediato.`}
                textoConfirmar="Sí, restablecer"
                destructivo
                cargando={procesando}
                onConfirmar={confirmarReset}
                onCancelar={() => setUsuarioAResetear(null)}
            />

            <ModalCredenciales
                open={Boolean(credenciales)}
                onClose={() => setCredenciales(null)}
                usuarioData={credenciales}
            />
        </Box>
    );
}
