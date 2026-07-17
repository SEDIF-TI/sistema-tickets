import React, { useContext, useState, useEffect } from 'react';
import { AuthContext } from '../context/AuthContext.jsx';
import { useNavigate } from 'react-router-dom';
import { 
    Box, Drawer, AppBar, Toolbar, List, Typography, Divider, 
    IconButton, ListItem, ListItemButton, ListItemIcon, Button,
    Badge, Snackbar, Alert, Tooltip, Menu, MenuItem, ListSubheader 
} from '@mui/material';

// ---> CORRECCIÓN 1: Importamos la API para que no explote el useEffect <---
import api from '../services/api';

// Iconos
import LogoutIcon from '@mui/icons-material/Logout';
import AssignmentIcon from '@mui/icons-material/Assignment';
import AddCircleIcon from '@mui/icons-material/AddCircle';
import NotificationsIcon from '@mui/icons-material/Notifications';
import DescriptionIcon from '@mui/icons-material/Description';
import AccountCircleIcon from '@mui/icons-material/AccountCircle'; 

const drawerWidth = 65; 
const COLOR_GUINDA = '#801A36';

export default function SoporteLayout({ children }) {
    const { user, logout } = useContext(AuthContext);
    const navigate = useNavigate();
    
    // Estados de Notificación de Soporte
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

    // 2. Alerta automática cuando llega un aviso nuevo
    useEffect(() => {
        if (avisos.length > 0) {
            const primerAviso = avisos[0]; 
            const ultimoIdMostrado = localStorage.getItem('ultimo_aviso_mostrado_id');

            if (ultimoIdMostrado !== String(primerAviso.id)) {
                setMensajeAviso(` ${primerAviso.titulo}: ${primerAviso.mensaje}`);
                setOpenAviso(true);
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

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    const userRole = user?.rol || user?.role || user?.rolNombre || '';
    const cleanRole = userRole.replace('ROLE_', '').toUpperCase();
    const estaBloqueado = user?.passwordTemporal; 

    return (
        <Box sx={{ display: 'flex' }}>
            
            {/* BARRA SUPERIOR */}
            <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1, bgcolor: COLOR_GUINDA }}>
                <Toolbar sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="h6" noWrap component="div" sx={{ fontWeight: 'bold' }}>
                        SEDIF - Sistema de Tickets
                    </Typography>

                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        
                        {/* ---> CORRECCIÓN 2: Cambiamos el onClick para que abra el menú <--- */}
                        {!estaBloqueado && (
                            <IconButton color="inherit" onClick={handleOpenHistorial}>
                                <Badge badgeContent={avisos.length} color="error">
                                    <NotificationsIcon />
                                </Badge>
                            </IconButton>
                        )}

                        <Typography variant="body2" sx={{ textTransform: 'uppercase' }}>
                            {user?.nombre || 'Usuario'} | {cleanRole}
                        </Typography>

                        {!estaBloqueado && (
                            <Tooltip title="Mi Perfil">
                                <IconButton color="inherit" onClick={() => navigate('/perfil')}>
                                    <AccountCircleIcon />
                                </IconButton>
                            </Tooltip>
                        )}

                        <Button color="inherit" onClick={handleLogout} startIcon={<LogoutIcon />}>
                            Salir
                        </Button>
                    </Box>
                </Toolbar>
            </AppBar>

            {/* MENÚ LATERAL ULTRA COMPACTO */}
            {!estaBloqueado && (
                <Drawer 
                    variant="permanent" 
                    sx={{ 
                        width: drawerWidth, 
                        flexShrink: 0, 
                        '& .MuiDrawer-paper': { 
                            width: drawerWidth, 
                            boxSizing: 'border-box',
                            overflowX: 'hidden',
                            backgroundColor: '#ffffff',
                            borderRight: '1px solid #e0e0e0'
                        } 
                    }}
                >
                    <Toolbar /> 
                    <List sx={{ pt: 2 }}>
                        <ListItem disablePadding sx={{ display: 'block', mb: 1 }}>
                            <Tooltip title="Mis Tickets" placement="right" arrow>
                                <ListItemButton onClick={() => navigate('/soporte/bandeja')} sx={{ justifyContent: 'center', px: 2.5, py: 1.5 }}>
                                    <ListItemIcon sx={{ minWidth: 0, justifyContent: 'center', color: COLOR_GUINDA }}>
                                        <AssignmentIcon />
                                    </ListItemIcon>
                                </ListItemButton>
                            </Tooltip>
                        </ListItem>
                        
                        <ListItem disablePadding sx={{ display: 'block', mb: 1 }}>
                            <Tooltip title="Levantar Ticket" placement="right" arrow>
                                <ListItemButton onClick={() => navigate('/tickets/nuevo')} sx={{ justifyContent: 'center', px: 2.5, py: 1.5 }}>
                                    <ListItemIcon sx={{ minWidth: 0, justifyContent: 'center', color: COLOR_GUINDA }}>
                                        <AddCircleIcon />
                                    </ListItemIcon>
                                </ListItemButton>
                            </Tooltip>
                        </ListItem>

                        <ListItem disablePadding sx={{ display: 'block', mb: 1 }}>
                            <Tooltip title="Generar Documento" placement="right" arrow>
                                <ListItemButton onClick={() => navigate('/documentos/crear')} sx={{ justifyContent: 'center', px: 2.5, py: 1.5 }}>
                                    <ListItemIcon sx={{ minWidth: 0, justifyContent: 'center', color: COLOR_GUINDA }}>
                                        <DescriptionIcon />
                                    </ListItemIcon>
                                </ListItemButton>
                            </Tooltip>
                        </ListItem>
                    </List>
                </Drawer>
            )}

            {/* CONTENIDO PRINCIPAL */}
            <Box component="main" sx={{ 
                flexGrow: 1, 
                p: 0, 
                mt: 8, 
                width: estaBloqueado ? '100%' : `calc(100% - ${drawerWidth}px)`
            }}>
                {children}
            </Box>

            {/* ---> CORRECCIÓN 3: Agregamos el diseño del menú desplegable <--- */}
            <Menu
                anchorEl={anchorEl}
                open={openHistorial}
                onClose={handleCloseHistorial}
                PaperProps={{
                    elevation: 3,
                    sx: { width: 320, maxHeight: 400, mt: 1.5, borderRadius: 2 }
                }}
            >
                <ListSubheader sx={{ bgcolor: 'white', fontWeight: 'bold', color: COLOR_GUINDA, lineHeight: '40px' }}>
                    Avisos Globales ({avisos.length})
                </ListSubheader>
                <Divider />
                
                {avisos.length === 0 ? (
                    <MenuItem onClick={handleCloseHistorial} sx={{ py: 2 }}>
                        <Typography variant="body2" color="textSecondary" sx={{ width: '100%', textAlign: 'center' }}>
                            No hay avisos recientes
                        </Typography>
                    </MenuItem>
                ) : (
                    avisos.map((aviso) => (
                        <MenuItem key={aviso.id} onClick={handleCloseHistorial} sx={{ whiteSpace: 'normal', py: 1.5, borderBottom: '1px solid #f0f0f0' }}>
                            <Box>
                                <Typography variant="subtitle2" fontWeight="bold">{aviso.titulo}</Typography>
                                <Typography variant="body2" color="textSecondary">{aviso.mensaje}</Typography>
                            </Box>
                        </MenuItem>
                    ))
                )}
            </Menu>

            {/* COMPONENTE DE ALERTA FLOTANTE (SNACKBAR) */}
            <Snackbar 
                open={openAviso} 
                autoHideDuration={8000} 
                onClose={handleCloseAviso}
                anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
            >
                <Alert 
                    onClose={handleCloseAviso} 
                    variant="filled" 
                    icon={false}
                    sx={{ 
                        minWidth: '400px', padding: '20px 30px', fontSize: '1.2rem', mt: 6, fontWeight: 'bold',
                        backgroundColor: COLOR_GUINDA, color: 'white'
                    }}
                >
                    <NotificationsIcon sx={{ mr: 2, verticalAlign: 'middle', fontSize: '2rem' }} />
                    {mensajeAviso}
                </Alert>
            </Snackbar>
        </Box>
    );
}