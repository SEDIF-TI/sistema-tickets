import React, { useContext, useEffect, useState } from 'react';
import { Box, Drawer, AppBar, Toolbar, List, Typography, ListItem, ListItemButton, ListItemIcon, Button, Tooltip, IconButton, CssBaseline, Alert } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext.jsx';
import api from '../services/api';

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
    
    // ---> ESTADOS PARA MÚLTIPLES AVISOS <---
    const [avisosActivos, setAvisosActivos] = useState([]);
    const [avisosOcultos, setAvisosOcultos] = useState([]); // Guarda los IDs que el usuario ya cerró

    const handleLogout = () => { logout(); navigate('/login'); };

    const userRole = user?.rol || user?.role || user?.rolNombre || '';
    const cleanRole = userRole.replace('ROLE_', '').toUpperCase();
    const estaBloqueado = user?.passwordTemporal;

    // Lógica para buscar avisos cada 30 segundos
    // Lógica para buscar avisos cada 10 segundos
    useEffect(() => {
        if (!user || estaBloqueado) return;

        const buscarAvisos = async () => {
            try {
                // Ajusta la ruta si tu api.js usa o no el /api
                const response = await api.get('/v1/avisos/activos');
                
                const paraMi = response.data.filter(a => {
                    const esActivo = a.activo === true;
                    
                    // AHORA EL ADMINISTRADOR VE TODOS LOS AVISOS, SIN IMPORTAR EL ÁREA
                    const esParaMi = !a.areaId || a.areaId === user?.areaId || cleanRole === 'ADMINISTRADOR';
                    
                    return esActivo && esParaMi;
                });
                
                setAvisosActivos(paraMi);
            } catch (error) {
                console.error("Error al buscar avisos:", error);
            }
        };

        buscarAvisos();
        
        // Bajamos el tiempo a 10 segundos (10000 ms) para que sea casi instantáneo
        const intervalo = setInterval(buscarAvisos, 10000); 
        return () => clearInterval(intervalo);
    }, [user, estaBloqueado, cleanRole]);

    // Función para ocultar un aviso específico cuando el usuario le da a la X
    const handleCerrarAviso = (idAviso) => {
        if (!avisosOcultos.includes(idAviso)) {
            setAvisosOcultos([...avisosOcultos, idAviso]);
        }
    };

    // Calculamos qué avisos pintar (los activos menos los que el usuario ya cerró)
    const avisosVisibles = avisosActivos.filter(aviso => !avisosOcultos.includes(aviso.id));

    return (
        <Box sx={{ display: 'flex', minHeight: '100vh', width: '100vw', bgcolor: '#f4f7f6', overflowX: 'hidden' }}>
            <CssBaseline />

            {/* ---> CONTENEDOR DE ALERTAS APILADAS <--- */}
            {avisosVisibles.length > 0 && (
                <Box sx={{ 
                    position: 'fixed', 
                    top: '75px', // Debajo de la barra superior
                    left: '50%', 
                    transform: 'translateX(-50%)', 
                    zIndex: 9999, 
                    display: 'flex', 
                    flexDirection: 'column', 
                    gap: 1.5, // Espacio entre avisos
                    width: '90%', 
                    maxWidth: '600px' 
                }}>
                    {avisosVisibles.map(aviso => (
                        <Alert 
                            key={aviso.id}
                            severity="warning" 
                            variant="filled" 
                            onClose={() => handleCerrarAviso(aviso.id)} 
                            sx={{ width: '100%', fontWeight: 'bold', boxShadow: 3 }}
                        >
                            {aviso.titulo}: {aviso.mensaje}
                        </Alert>
                    ))}
                </Box>
            )}

            <AppBar position="fixed" sx={{ width: '100%', bgcolor: COLOR_GUINDA, borderRadius: '0 !important', boxShadow: 2, zIndex: 1300 }}>
                <Toolbar sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="h6" fontWeight="bold">SEDIF - Sistema de Tickets</Typography>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        <Typography variant="body2" sx={{ textTransform: 'uppercase' }}>{user?.nombre || 'Usuario'} | {cleanRole}</Typography>
                        {!estaBloqueado && (
                            <Tooltip title="Mi Perfil"><IconButton color="inherit" onClick={() => navigate('/perfil')}><AccountCircleIcon /></IconButton></Tooltip>
                        )}
                        <Button color="inherit" onClick={handleLogout} startIcon={<ExitToAppIcon />}>Salir</Button>
                    </Box>
                </Toolbar>
            </AppBar>

            {user && !estaBloqueado && (
                <Drawer variant="permanent" sx={{ 
                    width: drawerWidth, flexShrink: 0, 
                    '& .MuiDrawer-paper': { width: drawerWidth, backgroundColor: '#ffffff', borderRight: '1px solid #e0e0e0', borderRadius: '0 !important' } 
                }}>
                    <Toolbar /> 
                    <List sx={{ pt: 2 }}>
                        {/* MENÚ ADMINISTRADOR */}
                        {cleanRole === 'ADMINISTRADOR' && (
                            <>
                                <ListItem disablePadding><Tooltip title="Dashboard" placement="right"><ListItemButton onClick={() => navigate('/admin/dashboard')} sx={{ justifyContent: 'center' }}><ListItemIcon sx={{ color: COLOR_GUINDA }}><DashboardIcon /></ListItemIcon></ListItemButton></Tooltip></ListItem>
                                <ListItem disablePadding><Tooltip title="Usuarios" placement="right"><ListItemButton onClick={() => navigate('/admin/usuarios')} sx={{ justifyContent: 'center' }}><ListItemIcon sx={{ color: COLOR_GUINDA }}><GroupIcon /></ListItemIcon></ListItemButton></Tooltip></ListItem>
                                <ListItem disablePadding><Tooltip title="Áreas" placement="right"><ListItemButton onClick={() => navigate('/admin/areas')} sx={{ justifyContent: 'center' }}><ListItemIcon sx={{ color: COLOR_GUINDA }}><DomainIcon /></ListItemIcon></ListItemButton></Tooltip></ListItem>
                                <ListItem disablePadding><Tooltip title="Avisos" placement="right"><ListItemButton onClick={() => navigate('/admin/avisos')} sx={{ justifyContent: 'center' }}><ListItemIcon sx={{ color: COLOR_GUINDA }}><CampaignIcon /></ListItemIcon></ListItemButton></Tooltip></ListItem>
                                <ListItem disablePadding><Tooltip title="Bitácora Global" placement="right"><ListItemButton onClick={() => navigate('/admin/bitacora')} sx={{ justifyContent: 'center' }}><ListItemIcon sx={{ color: COLOR_GUINDA }}><HistoryIcon /></ListItemIcon></ListItemButton></Tooltip></ListItem>
                            </>
                        )}
                        {/* MENÚ SOPORTE */}
                        {cleanRole === 'SOPORTE' && (
                            <>
                                <ListItem disablePadding><Tooltip title="Mis Tickets" placement="right"><ListItemButton onClick={() => navigate('/soporte/bandeja')} sx={{ justifyContent: 'center' }}><ListItemIcon sx={{ color: COLOR_GUINDA }}><AssignmentIcon /></ListItemIcon></ListItemButton></Tooltip></ListItem>
                                <ListItem disablePadding><Tooltip title="Levantar Ticket" placement="right"><ListItemButton onClick={() => navigate('/tickets/nuevo')} sx={{ justifyContent: 'center' }}><ListItemIcon sx={{ color: COLOR_GUINDA }}><AddCircleIcon /></ListItemIcon></ListItemButton></Tooltip></ListItem>
                                <ListItem disablePadding><Tooltip title="Crear Documento" placement="right"><ListItemButton onClick={() => navigate('/documentos/crear')} sx={{ justifyContent: 'center' }}><ListItemIcon sx={{ color: COLOR_GUINDA }}><DescriptionIcon /></ListItemIcon></ListItemButton></Tooltip></ListItem>
                            </>
                        )}
                        {/* MENÚ EMPLEADO */}
                        {cleanRole === 'EMPLEADO' && (
                            <>
                                <ListItem disablePadding><Tooltip title="Levantar Ticket" placement="right"><ListItemButton onClick={() => navigate('/tickets/nuevo')} sx={{ justifyContent: 'center' }}><ListItemIcon sx={{ color: COLOR_GUINDA }}><AddCircleIcon /></ListItemIcon></ListItemButton></Tooltip></ListItem>
                                <ListItem disablePadding><Tooltip title="Mis Tickets" placement="right"><ListItemButton onClick={() => navigate('/empleado/historial')} sx={{ justifyContent: 'center' }}><ListItemIcon sx={{ color: COLOR_GUINDA }}><HistoryIcon /></ListItemIcon></ListItemButton></Tooltip></ListItem>
                            </>
                        )}
                    </List>
                </Drawer>
            )}

            <Box component="main" sx={{ flexGrow: 1, p: 3, mt: '64px', width: '100%', ml: `${drawerWidth}px` }}>
                {children}
            </Box>
        </Box>
    );
}