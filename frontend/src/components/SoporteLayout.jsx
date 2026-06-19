import { useContext, useState, useEffect } from 'react';
import { AuthContext } from '../context/AuthContext.jsx';
import { useNavigate } from 'react-router-dom';
import { 
    Box, Drawer, AppBar, Toolbar, List, Typography, Divider, 
    IconButton, ListItem, ListItemButton, ListItemIcon, Button,
    Badge, Snackbar, Alert, Tooltip, Menu, MenuItem, ListSubheader 
} from '@mui/material';
import LogoutIcon from '@mui/icons-material/Logout';
import AssignmentIcon from '@mui/icons-material/Assignment';
import AddCircleIcon from '@mui/icons-material/AddCircle';
import NotificationsIcon from '@mui/icons-material/Notifications';
import DescriptionIcon from '@mui/icons-material/Description';
import api from '../services/api';

const drawerWidth = 70; // Ancho del menú

export default function SoporteLayout({ children }) {
    const { user, logout } = useContext(AuthContext);
    const navigate = useNavigate();
    
    // Estados de la notificación automática
    const [openAviso, setOpenAviso] = useState(false);
    const [mensajeAviso, setMensajeAviso] = useState('');
    const [avisos, setAvisos] = useState([]);

    // Estado para el menú flotante del historial (Campana)
    const [anchorEl, setAnchorEl] = useState(null);
    const openHistorial = Boolean(anchorEl);

    // 1. Cargar avisos desde el Backend
    useEffect(() => {
        const fetchAvisos = async () => {
            try {
                const respuesta = await api.get('/v1/avisos/activos');
                setAvisos(respuesta.data);
            } catch (error) {
                console.error("Error cargando avisos globales:", error);
            }
        };
        if (user) fetchAvisos();
    }, [user]);

    // 2. LA MAGIA: Alerta automática cuando llega un aviso nuevo
    useEffect(() => {
        if (avisos.length > 0) {
            const primerAviso = avisos[0]; // El más reciente
            
            // Revisamos cuál fue el último aviso que el navegador mostró automáticamente
            const ultimoIdMostrado = localStorage.getItem('ultimo_aviso_mostrado_id');

            // Si el ID es diferente, significa que es un aviso NUEVO que no ha visto
            if (ultimoIdMostrado !== String(primerAviso.id)) {
                setMensajeAviso(`📢 ${primerAviso.titulo}: ${primerAviso.mensaje}`);
                setOpenAviso(true);
                // Guardamos el ID para no volver a mostrárselo de golpe en el próximo render
                localStorage.setItem('ultimo_aviso_mostrado_id', primerAviso.id);
            }
        }
    }, [avisos]);

    // Manejadores para abrir y cerrar el historial de la campana
    const handleOpenHistorial = (event) => {
        setAnchorEl(event.currentTarget);
    };

    const handleCloseHistorial = () => {
        setAnchorEl(null);
    };

    const handleCloseAviso = () => {
        setOpenAviso(false);
    };

    return (
        <Box sx={{ display: 'flex' }}>
            
            {/* BARRA SUPERIOR (AppBar) */}
            <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}>
                <Toolbar sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="h6" noWrap component="div" sx={{ fontWeight: 'bold' }}>
                        SEDIF - Sistema de Tickets
                    </Typography>

                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        
                        {/* Campana: Al dar clic, abre el historial flotante */}
                        <IconButton color="inherit" onClick={handleOpenHistorial}>
                            <Badge badgeContent={avisos.length} color="error">
                                <NotificationsIcon />
                            </Badge>
                        </IconButton>

                        {/* --- MENÚ FLOTANTE DEL HISTORIAL DE AVISOS --- */}
                        <Menu
                            anchorEl={anchorEl}
                            open={openHistorial}
                            onClose={handleCloseHistorial}
                            PaperProps={{
                                sx: { maxWidth: 350, maxHeight: 400, mt: 1.5, borderRadius: 2 }
                            }}
                        >
                            <ListSubheader sx={{ fontWeight: 'bold', color: '#5c0a28', lineHeight: '40px' }}>
                                Historial de Comunicados
                            </ListSubheader>
                            <Divider />
                            {avisos.length === 0 ? (
                                <MenuItem onClick={handleCloseHistorial}>
                                    <Typography variant="body2" color="textSecondary">No hay avisos publicados</Typography>
                                </MenuItem>
                            ) : (
                                avisos.map((aviso) => (
                                    <MenuItem key={aviso.id} onClick={handleCloseHistorial} sx={{ display: 'block', py: 1.5, borderBottom: '1px solid #f0f0f0' }}>
                                        <Typography variant="subtitle2" fontWeight="bold" color="primary.main">
                                            {aviso.titulo}
                                        </Typography>
                                        <Typography variant="body2" sx={{ whiteSpace: 'normal', color: 'text.secondary', mt: 0.5 }}>
                                            {aviso.mensaje}
                                        </Typography>
                                    </MenuItem>
                                ))
                            )}
                        </Menu>
                        {/* --------------------------------------------- */}

                        {/* Usuario y Salir */}
                        <Typography variant="body2">
                            {user?.nombre || 'Usuario'} | {user?.rol || 'SOPORTE'}
                        </Typography>
                        <Button 
                            color="inherit" 
                            startIcon={<LogoutIcon />} 
                            onClick={() => { logout(); navigate('/login'); }}
                        >
                            Salir
                        </Button>
                    </Box>
                </Toolbar>
            </AppBar>

            {/* MENÚ LATERAL COLAPSADO (Drawer) */}
            <Drawer 
                variant="permanent" 
                sx={{ 
                    width: drawerWidth, 
                    flexShrink: 0, 
                    [`& .MuiDrawer-paper`]: { width: drawerWidth, overflowX: 'hidden' } 
                }}
            >
                <Toolbar />
                <Divider />
                <List>
                    <ListItem disablePadding sx={{ display: 'block' }}>
                        <Tooltip title="Mis Tickets" placement="right">
                            <ListItemButton onClick={() => navigate('/soporte/bandeja')} sx={{ justifyContent: 'center', py: 2 }}>
                                <ListItemIcon sx={{ minWidth: 0, justifyContent: 'center' }}>
                                    <AssignmentIcon color="primary" />
                                </ListItemIcon>
                            </ListItemButton>
                        </Tooltip>
                    </ListItem>
                    
                    <ListItem disablePadding sx={{ display: 'block' }}>
                        <Tooltip title="Levantar Ticket" placement="right">
                            <ListItemButton onClick={() => navigate('/soporte/nuevo')} sx={{ justifyContent: 'center', py: 2 }}>
                                <ListItemIcon sx={{ minWidth: 0, justifyContent: 'center' }}>
                                    <AddCircleIcon color="primary" />
                                </ListItemIcon>
                            </ListItemButton>
                        </Tooltip>
                    </ListItem>

                    <ListItem disablePadding sx={{ display: 'block' }}>
                        <Tooltip title="Generar Documento" placement="right">
                            <ListItemButton onClick={() => navigate('/documentos/crear')} sx={{ justifyContent: 'center', py: 2 }}>
                                <ListItemIcon sx={{ minWidth: 0, justifyContent: 'center' }}>
                                    <DescriptionIcon color="primary" />
                                </ListItemIcon>
                            </ListItemButton>
                        </Tooltip>
                    </ListItem>
                </List>
            </Drawer>

            {/* CONTENIDO PRINCIPAL (Children) */}
            <Box component="main" sx={{ flexGrow: 1, p: 3, mt: 8 }}>
                {children}
            </Box>

            {/* COMPONENTE DE NOTIFICACIÓN AUTOMÁTICA (Snackbar) */}
            <Snackbar 
                open={openAviso} 
                autoHideDuration={8000} 
                onClose={handleCloseAviso}
                anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
            >
                <Alert 
                    onClose={handleCloseAviso} 
                    variant="filled" 
                    sx={{ 
                        minWidth: '400px', 
                        padding: '20px 30px', 
                        fontSize: '1.2rem', 
                        mt: 6, 
                        fontWeight: 'bold',
                        backgroundColor: 'primary.main', 
                        color: 'white',
                        '& .MuiAlert-icon': { color: 'white', fontSize: '2rem', mr: 2 } 
                    }}
                >
                    {mensajeAviso}
                </Alert>
            </Snackbar>
        </Box>
    );
}