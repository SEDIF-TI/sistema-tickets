import { useContext, useEffect, useState } from 'react';
import { AuthContext } from '../context/AuthContext.jsx';
import { getIcon } from '../util/iconMapper.js';
import { useNavigate } from 'react-router-dom';

// 1. IMPORTACIONES DE DISEÑO (Material UI)
// Traemos todos los componentes visuales que arman la estructura de la página
import { 
    Box, Drawer, AppBar, Toolbar, List, Typography, ListItem, 
    ListItemButton, ListItemIcon, Tooltip, Divider,
    IconButton, Badge, Snackbar, Alert, Menu, MenuItem, ListSubheader, Button
} from '@mui/material';

// Importación de Íconos
import LogoutIcon from '@mui/icons-material/Logout';
import NotificationsIcon from '@mui/icons-material/Notifications';
import InfoIcon from '@mui/icons-material/Info';

// Importación de nuestra conexión al Backend
import api from '../services/api.js';

// Definimos el ancho del menú lateral para que el contenido principal sepa cuánto espacio dejar
const drawerWidth = 240;

export default function MainLayout({ children }) {
    // 2. HERRAMIENTAS DE NAVEGACIÓN Y SESIÓN
    // Extraemos los datos del usuario logueado y la función para cerrar sesión
    const { user, logout } = useContext(AuthContext);
    const navigate = useNavigate();

    // ==========================================
    // 3. ESTADOS DE LA CAMPANA DE AVISOS
    // ==========================================
    // Controla si el mensaje flotante gigante (Snackbar) está visible o no
    const [openAviso, setOpenAviso] = useState(false);
    // Guarda el texto exacto que se mostrará en ese mensaje flotante
    const [mensajeAviso, setMensajeAviso] = useState('');
    // Almacena la lista de avisos que nos devuelve la base de datos
    const [avisos, setAvisos] = useState([]);
    
    // Controla a qué elemento de la pantalla se "ancla" el menú desplegable del historial
    const [anchorEl, setAnchorEl] = useState(null);
    // Convierte el estado del ancla en un booleano (true si hay ancla, false si es null)
    const openHistorial = Boolean(anchorEl);

    // ==========================================
    // 4. CONSUMO DEL BACKEND (Cargar avisos)
    // ==========================================
    useEffect(() => {
        const fetchAvisos = async () => {
            try {
                // Hacemos la petición a Spring Boot para traer los avisos activos
                const respuesta = await api.get('/v1/avisos/activos');
                setAvisos(respuesta.data); // Los guardamos en nuestro estado
            } catch (error) {
                console.error("Error cargando avisos globales:", error);
            }
        };
        // Solo intentamos traer los avisos si hay un usuario logueado
        if (user) fetchAvisos();
    }, [user]);

    // ==========================================
    // 5. LÓGICA DEL POPUP AUTOMÁTICO (Anti-Spam)
    // ==========================================
    useEffect(() => {
        // Si la base de datos nos devolvió al menos un aviso...
        if (avisos.length > 0) {
            const primerAviso = avisos[0]; // Tomamos el más reciente
            
            // Revisamos en la memoria del navegador cuál fue el último aviso que le mostramos a este usuario
            const ultimoIdMostrado = localStorage.getItem('ultimo_aviso_mostrado_id');
            
            // Si el ID del aviso actual es diferente al último que vio, significa que es NUEVO
            if (ultimoIdMostrado !== String(primerAviso.id)) {
                // Armamos el mensaje, abrimos la alerta gigante y guardamos el ID para no volver a molestarlo
                setMensajeAviso(` ${primerAviso.titulo}: ${primerAviso.mensaje}`);
                setOpenAviso(true);
                localStorage.setItem('ultimo_aviso_mostrado_id', primerAviso.id);
            }
        }
    }, [avisos]); // Esto se ejecuta cada vez que el estado 'avisos' cambia

    // ==========================================
    // 6. FUNCIONES DE CONTROL VISUAL (Manejadores)
    // ==========================================
    // Abre el menú del historial anclándolo al botón que disparó el evento (la campana)
    const handleOpenHistorial = (event) => setAnchorEl(event.currentTarget);
    // Cierra el menú del historial quitando el ancla
    const handleCloseHistorial = () => setAnchorEl(null);
    // Cierra la alerta gigante de la parte inferior/superior
    const handleCloseAviso = () => setOpenAviso(false);

    // Extraemos las vistas permitidas del usuario. Si no tiene, devolvemos un arreglo vacío para evitar errores.
    const vistas = user?.vistas || [];

    return (
        <Box sx={{ display: 'flex' }}>
            
            {/* ========================================== */}
            {/* 7. BARRA SUPERIOR (AppBar)                 */}
            {/* ========================================== */}
            <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}>
                <Toolbar sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    
                    {/* Título de la aplicación */}
                    <Typography variant="h6" noWrap component="div" sx={{ fontWeight: 'bold' }}>
                        SEDIF - Sistema de Tickets
                    </Typography>
                    
                    {/* Panel derecho que agrupa la Campana, el Menú y la info del Usuario */}
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        
                        {/* Botón de la Campana de Notificaciones */}
                        <IconButton color="inherit" onClick={handleOpenHistorial}>
                            {/* El Badge crea el circulito rojo con el número total de avisos */}
                            <Badge badgeContent={avisos.length} color="error">
                                <NotificationsIcon />
                            </Badge>
                        </IconButton>

                        {/* Menú Desplegable: Historial de Avisos */}
                        <Menu
                            anchorEl={anchorEl}
                            open={openHistorial}
                            onClose={handleCloseHistorial}
                            PaperProps={{ sx: { maxWidth: 350, maxHeight: 400, mt: 1.5, borderRadius: 2 } }}
                        >
                            <ListSubheader sx={{ fontWeight: 'bold', color: '#5c0a28', lineHeight: '40px' }}>
                                Historial de Comunicados
                            </ListSubheader>
                            <Divider />
                            
                            {/* Si no hay avisos, mostramos un texto por defecto. Si hay, los mapeamos uno por uno */}
                            {avisos.length === 0 ? (
                                <MenuItem onClick={handleCloseHistorial}>
                                    <Typography variant="body2" color="textSecondary">No hay avisos publicados</Typography>
                                </MenuItem>
                            ) : (
                                avisos.map((aviso) => (
                                    <MenuItem key={aviso.id} onClick={handleCloseHistorial} sx={{ display: 'block', py: 1.5, borderBottom: '1px solid #f0f0f0' }}>
                                        <Typography variant="subtitle2" fontWeight="bold" color="primary.main">{aviso.titulo}</Typography>
                                        <Typography variant="body2" sx={{ whiteSpace: 'normal', color: 'text.secondary', mt: 0.5 }}>{aviso.mensaje}</Typography>
                                    </MenuItem>
                                ))
                            )}
                        </Menu>

                        {/* Nombre del Usuario y su Rol (quitando el prefijo ROLE_ si lo tiene) */}
                        <Typography variant="body2">
                            {user?.nombre || 'Usuario'} | {user?.rol?.replace('ROLE_', '') || 'ADMIN'}
                        </Typography>
                        
                        {/* Botón para cerrar sesión */}
                        <Button color="inherit" startIcon={<LogoutIcon />} onClick={() => { logout(); navigate('/login'); }}>
                            Salir
                        </Button>
                    </Box>
                </Toolbar>
            </AppBar>

            {/* ========================================== */}
            {/* 8. MENÚ LATERAL DINÁMICO (Drawer)          */}
            {/* ========================================== */}
            <Drawer variant="permanent" sx={{ width: drawerWidth, flexShrink: 0, [`& .MuiDrawer-paper`]: { width: drawerWidth, boxSizing: 'border-box', mt: 8 } }}>
                <List>
                    {/* Iteramos sobre las vistas que la base de datos le autorizó a este usuario */}
                    {vistas.map((vista, index) => {
                        // Buscamos el ícono correspondiente en el mapper. Si no existe, usamos InfoIcon como respaldo.
                        const Icono = getIcon(vista.icono) || InfoIcon;
                        return (
                            <ListItem key={index} disablePadding sx={{ display: 'block' }}>
                                {/* Tooltip muestra el nombre de la vista al pasar el mouse por encima */}
                                <Tooltip title={vista.nombre} placement="right">
                                    <ListItemButton onClick={() => navigate(vista.ruta)} sx={{ justifyContent: 'center', py: 2 }}>
                                        <ListItemIcon sx={{ minWidth: 0, justifyContent: 'center' }}>
                                            <Icono sx={{ color: '#5c0a28' }} />
                                        </ListItemIcon>
                                    </ListItemButton>
                                </Tooltip>
                            </ListItem>
                        );
                    })}
                </List>
                <Divider />
            </Drawer>

            {/* ========================================== */}
            {/* 9. CONTENEDOR PRINCIPAL                    */}
            {/* ========================================== */}
            <Box component="main" sx={{ flexGrow: 1, p: 3, mt: 8 }}>
                {/* Aquí es donde React Router inyecta la pantalla en la que estemos (ej. AdminUsuariosPage) */}
                {children}
            </Box>

            {/* ========================================== */}
            {/* 10. ALERTA GIGANTE AUTOMÁTICA (Snackbar)   */}
            {/* ========================================== */}
            {/* Este componente flota sobre toda la interfaz de manera independiente */}
            <Snackbar open={openAviso} autoHideDuration={8000} onClose={handleCloseAviso} anchorOrigin={{ vertical: 'top', horizontal: 'center' }}>
                <Alert onClose={handleCloseAviso} variant="filled" sx={{ minWidth: '400px', padding: '20px 30px', fontSize: '1.2rem', mt: 6, fontWeight: 'bold', backgroundColor: 'primary.main', color: 'white', '& .MuiAlert-icon': { color: 'white', fontSize: '2rem', mr: 2 } }}>
                    {mensajeAviso}
                </Alert>
            </Snackbar>
            
        </Box>
    );
}