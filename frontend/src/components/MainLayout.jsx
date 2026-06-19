import React, { useContext } from 'react';
import { Box, Drawer, AppBar, Toolbar, List, Typography, ListItem, ListItemButton, ListItemIcon, Button, Tooltip, IconButton } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext.jsx';

// Iconos
import ExitToAppIcon from '@mui/icons-material/ExitToApp';
import DashboardIcon from '@mui/icons-material/Dashboard';
import GroupIcon from '@mui/icons-material/Group';
import HistoryIcon from '@mui/icons-material/History';
import DomainIcon from '@mui/icons-material/Domain';
import AddCircleIcon from '@mui/icons-material/AddCircle';
import AccountCircleIcon from '@mui/icons-material/AccountCircle';

const drawerWidth = 65; 
const COLOR_GUINDA = '#801A36';

export default function MainLayout({ children }) {
    const { user, logout } = useContext(AuthContext);
    const navigate = useNavigate();

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    // Identificamos el rol exacto del usuario activo
    const userRole = user?.rol || user?.role || user?.rolNombre || '';
    const cleanRole = userRole.replace('ROLE_', '').toUpperCase();

    // --- NUEVA BARRERA VISUAL ---
    // Verificamos si el usuario está castigado en su primer inicio de sesión
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
                        <Typography variant="body2" sx={{ textTransform: 'uppercase' }}>
                            {user?.nombre || 'Usuario'} | {cleanRole}
                        </Typography>

                        {/* Ocultamos el botón de ir al perfil si ya está encerrado obligatoriamente en él */}
                        {!estaBloqueado && (
                            <Tooltip title="Mi Perfil">
                                <IconButton color="inherit" onClick={() => navigate('/perfil')}>
                                    <AccountCircleIcon />
                                </IconButton>
                            </Tooltip>
                        )}

                        <Button color="inherit" onClick={handleLogout} startIcon={<ExitToAppIcon />}>
                            Salir
                        </Button>
                    </Box>
                </Toolbar>
            </AppBar>

            {/* MENÚ LATERAL (Solo se dibuja si NO está bloqueado) */}
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
                        },
                    }}
                >
                    <Toolbar /> 
                    
                    <List sx={{ pt: 2 }}>
                        
                        {/* ---------------- MENÚ EXCLUSIVO DE ADMINISTRADOR ---------------- */}
                        {cleanRole === 'ADMINISTRADOR' && (
                            <>
                                <ListItem disablePadding sx={{ display: 'block', mb: 1 }}>
                                    <Tooltip title="Dashboard" placement="right" arrow>
                                        <ListItemButton onClick={() => navigate('/admin/dashboard')} sx={{ justifyContent: 'center', px: 2.5, py: 1.5 }}>
                                            <ListItemIcon sx={{ minWidth: 0, justifyContent: 'center', color: COLOR_GUINDA }}>
                                                <DashboardIcon />
                                            </ListItemIcon>
                                        </ListItemButton>
                                    </Tooltip>
                                </ListItem>

                                <ListItem disablePadding sx={{ display: 'block', mb: 1 }}>
                                    <Tooltip title="Usuarios" placement="right" arrow>
                                        <ListItemButton onClick={() => navigate('/admin/usuarios')} sx={{ justifyContent: 'center', px: 2.5, py: 1.5 }}>
                                            <ListItemIcon sx={{ minWidth: 0, justifyContent: 'center', color: COLOR_GUINDA }}>
                                                <GroupIcon />
                                            </ListItemIcon>
                                        </ListItemButton>
                                    </Tooltip>
                                </ListItem>

                                <ListItem disablePadding sx={{ display: 'block', mb: 1 }}>
                                    <Tooltip title="Áreas y Departamentos" placement="right" arrow>
                                        <ListItemButton onClick={() => navigate('/admin/areas')} sx={{ justifyContent: 'center', px: 2.5, py: 1.5 }}>
                                            <ListItemIcon sx={{ minWidth: 0, justifyContent: 'center', color: COLOR_GUINDA }}>
                                                <DomainIcon />
                                            </ListItemIcon>
                                        </ListItemButton>
                                    </Tooltip>
                                </ListItem>
                            </>
                        )}

                        {/* ---------------- MENÚ EXCLUSIVO DE EMPLEADO ---------------- */}
                        {cleanRole === 'EMPLEADO' && (
                            <>
                                <ListItem disablePadding sx={{ display: 'block', mb: 1 }}>
                                    <Tooltip title="Levantar Nuevo Ticket" placement="right" arrow>
                                        <ListItemButton onClick={() => navigate('/empleado/nuevo')} sx={{ justifyContent: 'center', px: 2.5, py: 1.5 }}>
                                            <ListItemIcon sx={{ minWidth: 0, justifyContent: 'center', color: COLOR_GUINDA }}>
                                                <AddCircleIcon />
                                            </ListItemIcon>
                                        </ListItemButton>
                                    </Tooltip>
                                </ListItem>
                            </>
                        )}

                        {/* ---------------- MENÚ COMPARTIDO (AMBOS LO VEN) ---------------- */}
                        <ListItem disablePadding sx={{ display: 'block', mb: 1 }}>
                            <Tooltip title={cleanRole === 'ADMINISTRADOR' ? "Bitácora Global" : "Mis Tickets"} placement="right" arrow>
                                <ListItemButton onClick={() => navigate('/empleado/historial')} sx={{ justifyContent: 'center', px: 2.5, py: 1.5 }}>
                                    <ListItemIcon sx={{ minWidth: 0, justifyContent: 'center', color: COLOR_GUINDA }}>
                                        <HistoryIcon />
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
                // Si el cajón izquierdo no existe, le damos el 100% del ancho a la pantalla
                width: estaBloqueado ? '100%' : `calc(100% - ${drawerWidth}px)` 
            }}>
                {children}
            </Box>
        </Box>
    );
}