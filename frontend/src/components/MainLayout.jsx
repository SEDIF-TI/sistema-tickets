import React, { useContext, useEffect, useState } from 'react';
import { Box, Drawer, AppBar, Toolbar, List, Typography, ListItem, ListItemButton, ListItemIcon, Button, Tooltip, IconButton, CssBaseline, Alert } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext.jsx';
import api from '../services/api';

// Importación de todos los iconos disponibles en tu sistema
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

const drawerWidth = 65; 
const COLOR_GUINDA = '#801A36';

// ✅ DICCIONARIO DE ÍCONOS: Vincula el texto de la Base de Datos con el componente de Material UI
const iconMap = {
    'DashboardIcon': DashboardIcon,
    'GroupIcon': GroupIcon,
    'HistoryIcon': HistoryIcon,
    'DomainIcon': DomainIcon,
    'AddCircleIcon': AddCircleIcon,
    'CampaignIcon': CampaignIcon,
    'AssignmentIcon': AssignmentIcon,
    'DescriptionIcon': DescriptionIcon,
    'ComputerIcon': ComputerIcon
};

export default function MainLayout({ children }) {
    const { user, logout } = useContext(AuthContext);
    const navigate = useNavigate();
    
    const [avisosActivos, setAvisosActivos] = useState([]);
    const [avisosOcultos, setAvisosOcultos] = useState([]); 

    const handleLogout = () => { logout(); navigate('/login'); };

    const userRole = user?.rol || user?.role || user?.rolNombre || '';
    const cleanRole = userRole.replace('ROLE_', '').toUpperCase();
    const estaBloqueado = user?.passwordTemporal;

    useEffect(() => {
        if (!user || estaBloqueado) return;

        const buscarAvisos = async () => {
            try {
                const response = await api.get('/v1/avisos/activos');
                const datosAvisos = Array.isArray(response.data) ? response.data : [];
                
                const paraMi = datosAvisos.filter(a => {
                    const esActivo = a.activo === true;
                    const esParaMi = !a.areaId || a.areaId === user?.areaId || cleanRole === 'ADMINISTRADOR';
                    return esActivo && esParaMi;
                });
                
                setAvisosActivos(paraMi);
                setAvisosOcultos([]); 
            } catch (error) {
                console.error("Error al buscar avisos:", error);
                setAvisosActivos([]);
            }
        };

        buscarAvisos();
        const intervalo = setInterval(buscarAvisos, 30000); 
        return () => clearInterval(intervalo);
    }, [user, estaBloqueado, cleanRole]);

    const handleCerrarAviso = (idAviso) => {
        if (!avisosOcultos.includes(idAviso)) {
            setAvisosOcultos([...avisosOcultos, idAviso]);
        }
    };

    const avisosVisibles = avisosActivos.filter(aviso => !avisosOcultos.includes(aviso.id));

    return (
        <Box sx={{ display: 'flex', minHeight: '100vh', width: '100vw', bgcolor: '#f4f7f6', overflowX: 'hidden' }}>
            <CssBaseline />

            {avisosVisibles.length > 0 && (
                <Box sx={{ 
                    position: 'fixed', top: '75px', left: '50%', 
                    transform: 'translateX(-50%)', zIndex: 9999, 
                    display: 'flex', flexDirection: 'column', gap: 1.5, 
                    width: '90%', maxWidth: '600px' 
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

            <AppBar position="fixed" sx={{ width: '100%', left: 0, top: 0, bgcolor: COLOR_GUINDA, borderRadius: '0 !important', boxShadow: 2, zIndex: 1300 }}>
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
                        
                        {/* ✅ RENDERIZADO COMPLETAMENTE DINÁMICO DESDE LA BASE DE DATOS */}
                        {(user?.vistasPermitidas || []).map((vista, index) => {
                            // Buscamos el componente del icono; si no existe en el mapa, usamos DescriptionIcon por defecto
                            const IconoDinamico = iconMap[vista.icono] || DescriptionIcon;

                            return (
                                <ListItem key={index} disablePadding>
                                    <Tooltip title={vista.nombre} placement="right">
                                        <ListItemButton 
                                            onClick={() => navigate(vista.ruta)} 
                                            sx={{ justifyContent: 'center' }}
                                        >
                                            <ListItemIcon sx={{ color: COLOR_GUINDA }}>
                                                <IconoDinamico />
                                            </ListItemIcon>
                                        </ListItemButton>
                                    </Tooltip>
                                </ListItem>
                            );
                        })}

                    </List>
                </Drawer>
            )}

            <Box component="main" sx={{ flexGrow: 1, p: 3, mt: '64px', width: '100%', ml: `${drawerWidth}px` }}>
                {children}
            </Box>
        </Box>
    );
}