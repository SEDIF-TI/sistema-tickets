import React, { useContext, useEffect, useState } from 'react'; // <-- AGREGA useEffect y useState
import { Box, Drawer, AppBar, Toolbar, List, Typography, ListItem, ListItemButton, ListItemIcon, Button, Tooltip, IconButton, CssBaseline, Snackbar, Alert } from '@mui/material'; // <-- AGREGA Snackbar y Alert
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext.jsx';
import api from '../services/api'; // <-- IMPORTA TU API PARA CONSULTAR LOS AVISOS

// Iconos
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

const drawerWidth = 65; 
const COLOR_GUINDA = '#801A36';

export default function MainLayout({ children }) {
    const { user, logout } = useContext(AuthContext);
    const navigate = useNavigate();

    // ---> ESTADOS PARA LOS AVISOS GLOBALES <---
    const [avisoActivo, setAvisoActivo] = useState(null);
    const [openAviso, setOpenAviso] = useState(false);

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    const userRole = user?.rol || user?.role || user?.rolNombre || '';
    const cleanRole = userRole.replace('ROLE_', '').toUpperCase();
    const estaBloqueado = user?.passwordTemporal;

    // ---> LÓGICA DE BÚSQUEDA CADA 30 SEGUNDOS <---
    useEffect(() => {
        if (!user || estaBloqueado) return;

        const buscarAvisos = async () => {
            try {
                const response = await api.get('/v1/avisos/activos');
                
                if (response.data && response.data.length > 0) {
                    setAvisoActivo(response.data[0]); 
                    setOpenAviso(true);
                } else {
                    // ✅ SI YA NO HAY AVISOS, LO APAGAMOS Y LO QUITAMOS DE LA PANTALLA
                    setAvisoActivo(null);
                    setOpenAviso(false);
                }
            } catch (error) {
                // ✅ SI EL BACKEND DEVUELVE ERROR (ej. 404 Not Found porque no hay avisos), TAMBIÉN LO APAGAMOS
                setAvisoActivo(null);
                setOpenAviso(false);
            }
        };

        buscarAvisos();

        const intervalo = setInterval(() => {
            buscarAvisos();
        }, 30000); // 30 segundos

        return () => clearInterval(intervalo);
    }, [user, estaBloqueado]);

    // Función para que el usuario cierre el aviso (pero volverá a salir en 30s si sigue activo)
    const handleCloseAviso = (event, reason) => {
        if (reason === 'clickaway') return;
        setOpenAviso(false);
    };

    return (
        <Box sx={{ display: 'flex', minHeight: '100vh', width: '100vw', bgcolor: '#f4f7f6', overflowX: 'hidden' }}>
            <CssBaseline />

            {/* ---> EL SNACKBAR QUE SALTARÁ CADA 30 SEGUNDOS <--- */}
            {avisoActivo && (
                <Snackbar 
                    open={openAviso} 
                    onClose={handleCloseAviso}
                    // No le ponemos autoHideDuration para forzar al usuario a cerrarlo o leerlo
                    anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
                    sx={{ mt: '65px', zIndex: 9999 }} // Se empuja hacia abajo para no tapar la AppBar superior
                >
                    <Alert 
                        onClose={handleCloseAviso} 
                        severity="warning" 
                        variant="filled"
                        sx={{ width: '100%', fontWeight: 'bold', fontSize: '1.05rem', boxShadow: 3 }}
                    >
                        {avisoActivo.titulo}: {avisoActivo.mensaje}
                    </Alert>
                </Snackbar>
            )}

            <AppBar position="fixed" sx={{ 
                width: '100%', 
                left: 0,
                top: 0,
                zIndex: (theme) => theme.zIndex.drawer + 1, 
                bgcolor: COLOR_GUINDA,
                borderRadius: '0 !important', 
                boxShadow: 2, 
                m: 0
            }}>
                <Toolbar sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="h6" noWrap component="div" sx={{ fontWeight: 'bold' }}>
                        SEDIF - Sistema de Tickets
                    </Typography>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
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
                        <Button color="inherit" onClick={handleLogout} startIcon={<ExitToAppIcon />}>
                            Salir
                        </Button>
                    </Box>
                </Toolbar>
            </AppBar>

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
                            borderRight: '1px solid #e0e0e0',
                            borderRadius: '0 !important',
                        },
                    }}
                >
                    <Toolbar /> 
                    <List sx={{ pt: 2 }}>
                        
                        {/* ---------------- MENU ADMINISTRADOR ---------------- */}
                        {cleanRole === 'ADMINISTRADOR' && (
                            <>
                                <ListItem disablePadding sx={{ display: 'block', mb: 1 }}>
                                    <Tooltip title="Dashboard" placement="right" arrow>
                                        <ListItemButton onClick={() => navigate('/admin/dashboard')} sx={{ justifyContent: 'center', px: 2.5, py: 1.5 }}>
                                            <ListItemIcon sx={{ minWidth: 0, justifyContent: 'center', color: COLOR_GUINDA }}><DashboardIcon /></ListItemIcon>
                                        </ListItemButton>
                                    </Tooltip>
                                </ListItem>
                                <ListItem disablePadding sx={{ display: 'block', mb: 1 }}>
                                    <Tooltip title="Usuarios" placement="right" arrow>
                                        <ListItemButton onClick={() => navigate('/admin/usuarios')} sx={{ justifyContent: 'center', px: 2.5, py: 1.5 }}>
                                            <ListItemIcon sx={{ minWidth: 0, justifyContent: 'center', color: COLOR_GUINDA }}><GroupIcon /></ListItemIcon>
                                        </ListItemButton>
                                    </Tooltip>
                                </ListItem>
                                <ListItem disablePadding sx={{ display: 'block', mb: 1 }}>
                                    <Tooltip title="Áreas y Departamentos" placement="right" arrow>
                                        <ListItemButton onClick={() => navigate('/admin/areas')} sx={{ justifyContent: 'center', px: 2.5, py: 1.5 }}>
                                            <ListItemIcon sx={{ minWidth: 0, justifyContent: 'center', color: COLOR_GUINDA }}><DomainIcon /></ListItemIcon>
                                        </ListItemButton>
                                    </Tooltip>
                                </ListItem>
                                <ListItem disablePadding sx={{ display: 'block', mb: 1 }}>
                                    <Tooltip title="Avisos Globales" placement="right" arrow>
                                        <ListItemButton onClick={() => navigate('/admin/avisos')} sx={{ justifyContent: 'center', px: 2.5, py: 1.5 }}>
                                            <ListItemIcon sx={{ minWidth: 0, justifyContent: 'center', color: COLOR_GUINDA }}><CampaignIcon /></ListItemIcon>
                                        </ListItemButton>
                                    </Tooltip>
                                </ListItem>
                                <ListItem disablePadding sx={{ display: 'block', mb: 1 }}>
                                    <Tooltip title="Bitácora Global" placement="right" arrow>
                                        <ListItemButton onClick={() => navigate('/empleado/historial')} sx={{ justifyContent: 'center', px: 2.5, py: 1.5 }}>
                                            <ListItemIcon sx={{ minWidth: 0, justifyContent: 'center', color: COLOR_GUINDA }}><HistoryIcon /></ListItemIcon>
                                        </ListItemButton>
                                    </Tooltip>
                                </ListItem>
                            </>
                        )}

                        {/* ---------------- MENU SOPORTE ---------------- */}
                        {cleanRole === 'SOPORTE' && (
                            <>
                                <ListItem disablePadding sx={{ display: 'block', mb: 1 }}>
                                    <Tooltip title="Mis Tickets" placement="right" arrow>
                                        <ListItemButton onClick={() => navigate('/soporte/bandeja')} sx={{ justifyContent: 'center', px: 2.5, py: 1.5 }}>
                                            <ListItemIcon sx={{ minWidth: 0, justifyContent: 'center', color: COLOR_GUINDA }}><AssignmentIcon /></ListItemIcon>
                                        </ListItemButton>
                                    </Tooltip>
                                </ListItem>

                                <ListItem disablePadding sx={{ display: 'block', mb: 1 }}>
                                    <Tooltip title="Levantar Ticket" placement="right" arrow>
                                        <ListItemButton onClick={() => navigate('/tickets/nuevo')} sx={{ justifyContent: 'center', px: 2.5, py: 1.5 }}>
                                            <ListItemIcon sx={{ minWidth: 0, justifyContent: 'center', color: COLOR_GUINDA }}><AddCircleIcon /></ListItemIcon>
                                        </ListItemButton>
                                    </Tooltip>
                                </ListItem>

                                <ListItem disablePadding sx={{ display: 'block', mb: 1 }}>
                                    <Tooltip title="Crear Documento" placement="right" arrow>
                                        <ListItemButton onClick={() => navigate('/documentos/crear')} sx={{ justifyContent: 'center', px: 2.5, py: 1.5 }}>
                                            <ListItemIcon sx={{ minWidth: 0, justifyContent: 'center', color: COLOR_GUINDA }}><DescriptionIcon /></ListItemIcon>
                                        </ListItemButton>
                                    </Tooltip>
                                </ListItem>
                            </>
                        )}

                        {/* ---------------- MENU EMPLEADO ---------------- */}
                        {cleanRole === 'EMPLEADO' && (
                            <>
                                <ListItem disablePadding sx={{ display: 'block', mb: 1 }}>
                                    <Tooltip title="Levantar Nuevo Ticket" placement="right" arrow>
                                        <ListItemButton onClick={() => navigate('/empleado/nuevo')} sx={{ justifyContent: 'center', px: 2.5, py: 1.5 }}>
                                            <ListItemIcon sx={{ minWidth: 0, justifyContent: 'center', color: COLOR_GUINDA }}><AddCircleIcon /></ListItemIcon>
                                        </ListItemButton>
                                    </Tooltip>
                                </ListItem>
                                <ListItem disablePadding sx={{ display: 'block', mb: 1 }}>
                                    <Tooltip title="Mis Tickets" placement="right" arrow>
                                        <ListItemButton onClick={() => navigate('/empleado/historial')} sx={{ justifyContent: 'center', px: 2.5, py: 1.5 }}>
                                            <ListItemIcon sx={{ minWidth: 0, justifyContent: 'center', color: COLOR_GUINDA }}><HistoryIcon /></ListItemIcon>
                                        </ListItemButton>
                                    </Tooltip>
                                </ListItem>
                            </>
                        )}
                    </List>
                </Drawer>
            )}

            <Box component="main" sx={{ 
                flexGrow: 1, 
                p: { xs: 2, md: 3 }, 
                mt: '64px', 
                width: '100%',
                minHeight: 'calc(100vh - 64px)',
                ml: estaBloqueado ? 0 : { xs: 0, md: `${drawerWidth}px` }, 
                boxSizing: 'border-box',
                display: 'flex',
                flexDirection: 'column',
                overflowX: 'hidden'
            }}>
                <Box sx={{ flexGrow: 1 }}>
                    {children}
                </Box>
                <Box component="footer" sx={{ py: 2, textAlign: 'center', bgcolor: '#ffffff', borderTop: '1px solid #e0e0e0', mt: 'auto' }}>
                    <Typography variant="body2" color="textSecondary" sx={{ fontWeight: '500' }}>
                        &copy; {new Date().getFullYear()} SEDIF Puebla - Sistema de Tickets. Todos los derechos reservados.
                    </Typography>
                </Box>
            </Box>
        </Box>
    );
}