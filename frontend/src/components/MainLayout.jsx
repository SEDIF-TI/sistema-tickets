import { useContext, useEffect, useState, useMemo } from 'react';
import {
    Box, Drawer, AppBar, Toolbar, List, Typography, ListItem, ListItemButton,
    ListItemIcon, ListItemText, Button, Tooltip, IconButton, Alert,
    useMediaQuery, Collapse
} from '@mui/material';
import { useTheme } from '@mui/material/styles';
import { useNavigate, useLocation } from 'react-router-dom';

import { AuthContext } from '../context/AuthContext.jsx';
import { useWebSocket } from '../context/useWebSocket.js';
import { avisoService } from '../services/avisoService';
import { perfilService } from '../services/perfilService';
import { useNetworkStatus } from '../hooks/useNetworkStatus.jsx';

import PieDePagina from './PieDePagina.jsx';

import logoPuebla from '../assets/logo-puebla.png';

// Iconos
import MenuIcon from '@mui/icons-material/Menu';
import ExitToAppIcon from '@mui/icons-material/ExitToApp';
import DashboardIcon from '@mui/icons-material/Dashboard';
import GroupIcon from '@mui/icons-material/Group';
import HistoryIcon from '@mui/icons-material/History';
import DomainIcon from '@mui/icons-material/Domain';
import AddCircleIcon from '@mui/icons-material/AddCircle';
import AccountCircleIcon from '@mui/icons-material/AccountCircle';
import CampaignIcon from '@mui/icons-material/Campaign';
import AssignmentIcon from '@mui/icons-material/Assignment';
import DescriptionIcon from '@mui/icons-material/Description';
import ComputerIcon from '@mui/icons-material/Computer';
import WifiOffIcon from '@mui/icons-material/WifiOff';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import InventoryIcon from '@mui/icons-material/Inventory';
import HomeRepairServiceIcon from '@mui/icons-material/HomeRepairService';
import MailIcon from '@mui/icons-material/Mail';

/** Ancho del menú lateral en escritorio (solo iconos). */
const ANCHO_MENU = 72;

/** Ancho del cajón en móvil, donde sí caben las etiquetas. */
const ANCHO_MENU_MOVIL = 268;

/** Alto de la barra superior, que aloja el logotipo institucional. */
const ALTO_BARRA = { xs: 56, sm: 68 };

/**
 * Alto del logotipo: deliberadamente cercano al de la barra.
 *
 * Los 4px que restan por lado son el respiro minimo para que no parezca
 * recortado; con la barra mas delgada, un logotipo pequeno dejaba franjas
 * vacias arriba y abajo.
 */
const ALTO_LOGO = { xs: 48, sm: 60 };

/**
 * Traduce el nombre de icono guardado en la tabla `vista` a su componente.
 * El menú se construye desde la base de datos, así que este mapa es el punto
 * de unión entre esa configuración y la interfaz.
 */
const ICONOS = {
    DashboardIcon,
    GroupIcon,
    HistoryIcon,
    DomainIcon,
    AddCircleIcon,
    CampaignIcon,
    AssignmentIcon,
    DescriptionIcon,
    ComputerIcon,
    InventoryIcon,
    HomeRepairServiceIcon,
    MailIcon,
};

/**
 * Estructura principal de la aplicación: barra superior, menú lateral y área
 * de contenido.
 *
 * Correcciones respecto a la versión anterior:
 *  - El contenido combinaba `width: 100%` con `marginLeft: 72px`, lo que
 *    garantizaba desbordamiento horizontal en cualquier resolución.
 *  - El menú era `variant="permanent"` sin alternativa móvil: robaba 72px de
 *    ancho en pantallas de 375px, donde no sobra ni uno.
 *  - Los avisos flotaban sobre el contenido en posición fija, tapándolo, y con
 *    `variant="filled"` de severidad warning quedaban en 3.19:1 de contraste,
 *    por debajo del mínimo legible.
 *  - Los botones del menú solo tenían icono, sin nombre accesible: un lector
 *    de pantalla anunciaba "botón" en cada opción.
 *  - El aviso de desconexión solo se mostraba al rol EMPLEADO, cuando afecta
 *    igual a todos.
 */
export default function MainLayout({ children }) {
    const { user, logout, actualizarVistas } = useContext(AuthContext);
    // El contexto expone `isConnected`. Antes se desestructuraba `connected`,
    // que no existe: valía siempre `undefined` y la suscripción de avisos de
    // más abajo nunca llegaba a activarse.
    const { stompClient, isConnected } = useWebSocket();
    const navigate = useNavigate();
    const location = useLocation();
    const theme = useTheme();
    const esEscritorio = useMediaQuery(theme.breakpoints.up('md'));

    const [menuAbierto, setMenuAbierto] = useState(false);
    const [avisos, setAvisos] = useState([]);
    const [avisosOcultos, setAvisosOcultos] = useState([]);
    const [mostrarRecuperacion, setMostrarRecuperacion] = useState(false);
    const [estuvoSinConexion, setEstuvoSinConexion] = useState(false);

    const estadoRed = useNetworkStatus();
    const sinConexion = Boolean(estadoRed.sinConexion ?? estadoRed);

    const rolCrudo = user?.rol || user?.role || user?.rolNombre || '';
    const rol = rolCrudo.replace('ROLE_', '').toUpperCase();
    const bloqueadoPorPassword = Boolean(user?.passwordTemporal);

    const vistas = useMemo(() => user?.vistasPermitidas ?? [], [user]);
    const mostrarMenu = Boolean(user) && !bloqueadoPorPassword && vistas.length > 0;

    // --- Recuperación de conexión -----------------------------------------
    useEffect(() => {
        if (sinConexion) {
            setEstuvoSinConexion(true);
            setMostrarRecuperacion(false);
        } else if (estuvoSinConexion) {
            setMostrarRecuperacion(true);
            setEstuvoSinConexion(false);
            const temporizador = setTimeout(() => setMostrarRecuperacion(false), 5000);
            return () => clearTimeout(temporizador);
        }
    }, [sinConexion, estuvoSinConexion]);

    // --- Menú vigente ------------------------------------------------------
    // El menú viaja en la respuesta del login y queda guardado en el
    // navegador. Se refresca al entrar para que una vista retirada deje de
    // dibujarse sin necesidad de cerrar sesión.
    useEffect(() => {
        if (!user || bloqueadoPorPassword) return;

        let cancelado = false;

        perfilService.getVistas()
            .then((respuesta) => {
                if (!cancelado && Array.isArray(respuesta?.data)) {
                    actualizarVistas(respuesta.data);
                }
            })
            .catch(() => {
                // Sin respuesta se conserva el menú guardado: es preferible uno
                // desactualizado a dejar al usuario sin navegación.
            });

        return () => { cancelado = true; };
        // Solo al montar y al cambiar de sesión: `actualizarVistas` cambia el
        // usuario, y depender de ella dispararía el efecto en bucle.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [user?.usuarioId, bloqueadoPorPassword]);

    // --- Avisos institucionales -------------------------------------------
    useEffect(() => {
        if (!user || bloqueadoPorPassword) return;

        const buscarAvisos = async () => {
            // Sin conexión no se insiste: solo llenaría la consola de errores.
            if (sinConexion) return;

            try {
                const respuesta = await avisoService.getActivos();
                const datos = Array.isArray(respuesta.data) ? respuesta.data : [];

                setAvisos(
                    datos.filter((a) => {
                        const activo = a.activo === true;
                        const esParaMi =
                            !a.areaId || a.areaId === user?.areaId || rol === 'ADMINISTRADOR';
                        return activo && esParaMi;
                    })
                );
            } catch {
                // El fallo ya se refleja en el indicador de conexión.
                setAvisos([]);
            }
        };

        buscarAvisos();
        const intervalo = setInterval(buscarAvisos, 30000);
        return () => clearInterval(intervalo);
    }, [user, bloqueadoPorPassword, rol, sinConexion]);

    // --- Alertas de resguardos vencidos en tiempo real ---------------------
    useEffect(() => {
        if (!stompClient || !isConnected || !user || bloqueadoPorPassword) return;

        const suscripcion = stompClient.subscribe('/topic/alertas-resguardos', (mensaje) => {
            try {
                const resguardo = JSON.parse(mensaje.body);
                setAvisos((prev) => [
                    {
                        id: `resguardo-${resguardo.id}-${Date.now()}`,
                        titulo: 'Resguardo vencido',
                        mensaje: `El resguardo de ${resguardo.solicitanteNombre} (${resguardo.equipoNombre}) ha vencido.`,
                        severidad: 'error',
                    },
                    ...prev,
                ]);
            } catch {
                // Un mensaje mal formado no debe tumbar la suscripción.
            }
        });

        return () => suscripcion?.unsubscribe();
    }, [stompClient, isConnected, user, bloqueadoPorPassword]);

    const avisosVisibles = avisos.filter((a) => !avisosOcultos.includes(a.id));

    // --- Acciones ----------------------------------------------------------
    const cerrarSesion = () => {
        logout();
        navigate('/login');
    };

    const irA = (ruta) => {
        navigate(ruta);
        if (!esEscritorio) setMenuAbierto(false);
    };

    // --- Menú lateral ------------------------------------------------------
    const contenidoMenu = (
        <>
            <Toolbar sx={{ minHeight: ALTO_BARRA }} />
            <List component="nav" aria-label="Navegación principal" sx={{ px: 1, pt: 2 }}>
                {vistas.map((vista) => {
                    const Icono = ICONOS[vista.icono] || DescriptionIcon;
                    const activa = location.pathname === vista.ruta;

                    return (
                        <ListItem key={vista.ruta} disablePadding sx={{ mb: 0.5 }}>
                            <Tooltip title={esEscritorio ? vista.nombre : ''} placement="right" arrow>
                                <ListItemButton
                                    onClick={() => irA(vista.ruta)}
                                    selected={activa}
                                    // El tooltip no aporta nombre accesible: sin
                                    // esto el lector de pantalla solo diría "botón".
                                    aria-label={vista.nombre}
                                    aria-current={activa ? 'page' : undefined}
                                    sx={{
                                        minHeight: 48,
                                        justifyContent: esEscritorio ? 'center' : 'flex-start',
                                        px: esEscritorio ? 1 : 2,
                                    }}
                                >
                                    <ListItemIcon
                                        sx={{
                                            minWidth: esEscritorio ? 0 : 40,
                                            justifyContent: 'center',
                                            color: activa ? 'primary.main' : 'text.secondary',
                                        }}
                                    >
                                        <Icono />
                                    </ListItemIcon>
                                    {/* La etiqueta solo se muestra en el cajón
                                        móvil, donde hay espacio de sobra. */}
                                    {!esEscritorio && (
                                        <ListItemText
                                            primary={vista.nombre}
                                            primaryTypographyProps={{
                                                fontWeight: activa ? 600 : 500,
                                                color: activa ? 'primary.main' : 'text.primary',
                                            }}
                                        />
                                    )}
                                </ListItemButton>
                            </Tooltip>
                        </ListItem>
                    );
                })}
            </List>
        </>
    );

    return (
        <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: 'background.default' }}>
            <a href="#contenido-principal" className="skip-link">
                Saltar al contenido principal
            </a>

            {/* --- Barra superior --- */}
            <AppBar
                position="fixed"
                sx={{ bgcolor: 'primary.main', zIndex: (t) => t.zIndex.drawer + 1 }}
            >
                <Toolbar sx={{ minHeight: ALTO_BARRA, gap: 1, px: { xs: 1, sm: 3 }, position: 'relative' }}>
                    {mostrarMenu && !esEscritorio && (
                        <IconButton
                            color="inherit"
                            edge="start"
                            onClick={() => setMenuAbierto(true)}
                            aria-label="Abrir menú de navegación"
                        >
                            <MenuIcon />
                        </IconButton>
                    )}

                    {/* Logotipo institucional.

                        En escritorio se centra respecto a la BARRA, no respecto
                        al espacio libre: colocado en el flujo, los bloques de
                        los lados tienen anchos distintos —el de la izquierda
                        aparece solo en movil— y el logo quedaba desplazado. Con
                        posicion absoluta el centro es siempre el de la pantalla.

                        `pointerEvents: none` evita que la caja invisible tape
                        los botones que quedan debajo. */}
                    <Box
                        sx={{
                            position: { xs: 'static', md: 'absolute' },
                            left: { md: '50%' },
                            transform: { md: 'translateX(-50%)' },
                            flex: { xs: 1, md: 'unset' },
                            display: 'flex',
                            justifyContent: { xs: 'flex-start', md: 'center' },
                            alignItems: 'center',
                            pointerEvents: 'none',
                        }}
                    >
                        <Box
                            component="img"
                            src={logoPuebla}
                            alt="Gobierno del Estado de Puebla"
                            sx={{
                                height: ALTO_LOGO,
                                width: 'auto',
                                objectFit: 'contain',
                                // El logotipo es oscuro; sobre el guinda se
                                // invierte a blanco para que se lea.
                                filter: 'brightness(0) invert(1)',
                            }}
                        />
                    </Box>

                    {/* Empuja la identidad del usuario al extremo derecho, ya
                        que el logotipo salio del flujo en escritorio. */}
                    <Box sx={{ flex: 1, display: { xs: 'none', md: 'block' } }} />

                    {/* Identidad del usuario y acciones. */}
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: { xs: 0.5, sm: 1 } }}>
                        <Box sx={{ display: { xs: 'none', md: 'block' }, textAlign: 'right', mr: 1 }}>
                            <Typography variant="body2" sx={{ lineHeight: 1.3, fontWeight: 500 }}>
                                {user?.nombre || 'Usuario'}
                            </Typography>
                            <Typography variant="caption" sx={{ opacity: 0.85 }}>
                                {rol}
                            </Typography>
                        </Box>

                        {!bloqueadoPorPassword && (
                            <Tooltip title="Mi perfil">
                                <IconButton
                                    color="inherit"
                                    onClick={() => navigate('/perfil')}
                                    aria-label="Ir a mi perfil"
                                >
                                    <AccountCircleIcon />
                                </IconButton>
                            </Tooltip>
                        )}

                        <Button
                            color="inherit"
                            onClick={cerrarSesion}
                            startIcon={<ExitToAppIcon />}
                            sx={{ display: { xs: 'none', sm: 'inline-flex' } }}
                        >
                            Salir
                        </Button>

                        <Tooltip title="Cerrar sesión">
                            <IconButton
                                color="inherit"
                                onClick={cerrarSesion}
                                aria-label="Cerrar sesión"
                                sx={{ display: { xs: 'inline-flex', sm: 'none' } }}
                            >
                                <ExitToAppIcon />
                            </IconButton>
                        </Tooltip>
                    </Box>
                </Toolbar>
            </AppBar>

            {/* --- Menú lateral ---
                Escritorio: fijo y estrecho. Móvil: cajón temporal sobre el
                contenido, para no robarle ancho a la pantalla. */}
            {mostrarMenu && (
                <Box component="nav" sx={{ width: { md: ANCHO_MENU }, flexShrink: { md: 0 } }}>
                    <Drawer
                        variant={esEscritorio ? 'permanent' : 'temporary'}
                        open={esEscritorio ? true : menuAbierto}
                        onClose={() => setMenuAbierto(false)}
                        ModalProps={{ keepMounted: true }}
                        sx={{
                            '& .MuiDrawer-paper': {
                                width: esEscritorio ? ANCHO_MENU : ANCHO_MENU_MOVIL,
                                boxSizing: 'border-box',
                                bgcolor: 'background.paper',
                                borderRight: '1px solid',
                                borderColor: 'divider',
                            },
                        }}
                    >
                        {contenidoMenu}
                    </Drawer>
                </Box>
            )}

            {/* --- Contenido ---
                `minWidth: 0` permite que una tabla ancha genere su propio
                scroll interno en lugar de estirar toda la página. */}
            <Box
                component="main"
                id="contenido-principal"
                sx={{
                    flexGrow: 1,
                    minWidth: 0,
                    // Columna con alto minimo de pantalla: deja que el pie use
                    // `mt: auto` para bajar al fondo cuando el contenido es
                    // corto, sin fijarlo con `position`.
                    display: 'flex',
                    flexDirection: 'column',
                    minHeight: '100vh',
                    p: { xs: 2, sm: 3 },
                    // En píxeles, no con el valor suelto de ALTO_BARRA: `mt`
                    // interpreta los números como múltiplos del espaciado del
                    // tema (8px), de modo que `mt: {xs: 72, sm: 88}` reservaba
                    // 576px y 704px en lugar de 72 y 88. Ese era el bloque en
                    // blanco que aparecía sobre el contenido en todas las
                    // pantallas, empujándolo por debajo del pliegue.
                    mt: { xs: `${ALTO_BARRA.xs}px`, sm: `${ALTO_BARRA.sm}px` },
                }}
            >
                {/* Estado de la conexión. `aria-live` hace que un lector de
                    pantalla lo anuncie sin que el usuario tenga que buscarlo. */}
                <Box aria-live="polite">
                    <Collapse in={sinConexion}>
                        <Alert severity="error" icon={<WifiOffIcon />} sx={{ mb: 2 }}>
                            {estadoRed.servidorCaido
                                ? 'No hay comunicación con el servidor. Los datos que ves pueden estar desactualizados.'
                                : 'Sin conexión a internet. Revisa tu red para continuar trabajando.'}
                        </Alert>
                    </Collapse>

                    <Collapse in={mostrarRecuperacion}>
                        <Alert
                            severity="success"
                            icon={<CheckCircleIcon />}
                            onClose={() => setMostrarRecuperacion(false)}
                            sx={{ mb: 2 }}
                        >
                            Conexión restablecida.
                        </Alert>
                    </Collapse>
                </Box>

                {/* Avisos institucionales, en línea y no flotando sobre el
                    contenido. Severidad estándar, no `filled`, para respetar el
                    contraste mínimo. */}
                {avisosVisibles.length > 0 && (
                    <Box
                        aria-live="polite"
                        sx={{ display: 'flex', flexDirection: 'column', gap: 1.5, mb: 3 }}
                    >
                        {avisosVisibles.map((aviso) => (
                            <Alert
                                key={aviso.id}
                                severity={aviso.severidad || 'warning'}
                                onClose={() => setAvisosOcultos((prev) => [...prev, aviso.id])}
                                className="fade-in-up"
                            >
                                <Box component="strong" sx={{ fontWeight: 600 }}>
                                    {aviso.titulo}
                                </Box>
                                {aviso.mensaje ? `: ${aviso.mensaje}` : ''}
                            </Alert>
                        ))}
                    </Box>
                )}

                {/* El contenido crece para ocupar el hueco disponible, de modo
                    que el pie quede abajo aunque la pantalla tenga poco que
                    mostrar. */}
                <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                    {children}
                </Box>

                <PieDePagina />
            </Box>
        </Box>
    );
}
