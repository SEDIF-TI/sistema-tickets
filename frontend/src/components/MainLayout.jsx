import React, { useContext, useEffect, useState } from 'react';
import { Box, Drawer, AppBar, Toolbar, List, Typography, ListItem, ListItemButton, ListItemIcon, Button, Tooltip, IconButton, CssBaseline, Alert } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext.jsx';
import api from '../services/api';

// --- custom hook ---
import { useNetworkStatus } from '../hooks/useNetworkStatus.jsx';

// 1. Importar el logo
import logoPuebla from '../assets/logo-puebla.png'; 

// Importación de iconos
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
import CloseIcon from '@mui/icons-material/Close';

const drawerWidth = 65; 
const COLOR_GUINDA = '#801A36';

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

    // --- 1. Usar el custom hook que ya importaste ---
    const isOffline = useNetworkStatus();

    // --- 2. Estados para la recuperación de red (mensaje verde) ---
    const [wasOffline, setWasOffline] = useState(false);
    const [mostrarRecuperacion, setMostrarRecuperacion] = useState(false);

    const handleLogout = () => { logout(); navigate('/login'); };

    const userRole = user?.rol || user?.role || user?.rolNombre || '';
    const cleanRole = userRole.replace('ROLE_', '').toUpperCase();
    const estaBloqueado = user?.passwordTemporal;

    // --- 3. Efecto para mostrar alerta cuando regresa el internet ---
    useEffect(() => {
        if (isOffline) {
            setWasOffline(true);           
            setMostrarRecuperacion(false); 
        } else if (!isOffline && wasOffline) {
            setMostrarRecuperacion(true);
            setWasOffline(false);
            const timer = setTimeout(() => setMostrarRecuperacion(false), 5000);
            return () => clearTimeout(timer);
        }
    }, [isOffline, wasOffline]);
    // --------------------------------------------------------------

    useEffect(() => {
        if (!user || estaBloqueado) return;

        const buscarAvisos = async () => {
            // Si el sistema está offline, detenemos la petición para no saturar la red ni la consola
            if (isOffline) return;
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
    }, [user, estaBloqueado, cleanRole, isOffline]);

    const handleCerrarAviso = (idAviso) => {
        if (!avisosOcultos.includes(idAviso)) {
            setAvisosOcultos([...avisosOcultos, idAviso]);
        }
    };

    const avisosVisibles = avisosActivos.filter(aviso => !avisosOcultos.includes(aviso.id));

    return (
        <Box sx={{ display: 'flex', minHeight: '100vh', width: '100vw', bgcolor: '#f4f7f6', overflowX: 'hidden' }}>
            <CssBaseline />

            {/* --- Renderizado del Banner Offline PWA (ROJO) --- */}
            {isOffline && cleanRole === 'EMPLEADO' && (
                <Box sx={{ position: 'fixed', top: '75px', left: '50%', transform: 'translateX(-50%)', zIndex: 9999, display: 'flex', flexDirection: 'column', width: '90%', maxWidth: '600px', mt: 2 }}>
                    <Alert severity="error" variant="filled" icon={<WifiOffIcon />} sx={{ width: '100%', fontWeight: 'bold', boxShadow: 3 }}>
                        Estás navegando sin conexión. Funcionalidad de reportes suspendida.
                    </Alert>
                </Box>
            )}
            {/* ------------------------------------------------- */}

           {/* --- Renderizado del Banner de Recuperación (VERDE SUAVE) --- */}
            {mostrarRecuperacion && cleanRole === 'EMPLEADO' && (
                <Box sx={{ position: 'fixed', top: '75px', left: '50%', transform: 'translateX(-50%)', zIndex: 9999, width: '90%', maxWidth: '600px', mt: 2 }}>
                    <Box sx={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        bgcolor: '#edf7ed', // <-- Fondo verde pastel suave
                        color: '#1e4620',   // <-- Letras en verde oscuro
                        px: 2,
                        py: 1.5,
                        borderRadius: '4px',
                        boxShadow: 3
                    }}>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                            <CheckCircleIcon sx={{ color: '#1e4620' }} /> {/* <-- Icono verde oscuro */}
                            <Typography sx={{ fontWeight: 'bold', fontSize: '0.875rem' }}>
                                Conexión recuperada. Ya puedes continuar navegando y generando reportes.
                            </Typography>
                        </Box>
                        <IconButton size="small" onClick={() => setMostrarRecuperacion(false)} sx={{ color: '#1e4620', p: 0.5 }}>
                            <CloseIcon fontSize="small" />
                        </IconButton>
                    </Box>
                </Box>
            )}
            {/* ----------------------------------------------------------------------- */}

            {avisosVisibles.length > 0 && (
                <Box sx={{ position: 'fixed', top: '75px', left: '50%', transform: 'translateX(-50%)', zIndex: 9998, display: 'flex', flexDirection: 'column', gap: 1.5, width: '90%', maxWidth: '600px', mt: (isOffline || mostrarRecuperacion) && cleanRole === 'EMPLEADO' ? 8 : 0 }}>
                    {avisosVisibles.map(aviso => (
                        <Alert key={aviso.id} severity="warning" variant="filled" onClose={() => handleCerrarAviso(aviso.id)} sx={{ width: '100%', fontWeight: 'bold', boxShadow: 3 }}>
                            {aviso.titulo}: {aviso.mensaje}
                        </Alert>
                    ))}
                </Box>
            )}

            <AppBar position="fixed" sx={{ width: '100%', left: 0, top: 0, bgcolor: COLOR_GUINDA, borderRadius: '0 !important', boxShadow: 2, zIndex: 1300 }}>
                <Toolbar sx={{ 
                    display: 'flex', 
                    justifyContent: 'space-between', 
                    alignItems: 'center', 
                    minHeight: { xs: '80px', sm: '95px' },  //barra superior
                    px: { xs: 1, sm: 3 } 
                }}>
                    
                    {/* 1. SECCIÓN IZQUIERDA */}
                    <Box sx={{ flex: 1, display: 'flex', justifyContent: 'flex-start' }}></Box>

                    {/* 2. SECCIÓN CENTRAL (Logo Mucho Más Grande) */}
                    <Box sx={{ flex: 2, display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
                        <img 
                            src={logoPuebla} 
                            alt="Logo Puebla" 
                            style={{ 
                                height: '108px', //logo
                                width: 'auto', 
                                maxWidth: '100%', 
                                objectFit: 'contain',
                                filter: 'brightness(0) invert(1)' 
                            }} 
                        />
                    </Box>

                    {/* 3. SECCIÓN DERECHA */}
                    <Box sx={{ flex: 1, display: 'flex', justifyContent: 'flex-end', alignItems: 'center', gap: { xs: 0.5, sm: 1.5 } }}>
                        <Typography 
                            variant="body2" 
                            sx={{ textTransform: 'uppercase', display: { xs: 'none', md: 'block' }, textAlign: 'right', lineHeight: 1.2 }}
                        >
                            {user?.nombre || 'Usuario'} <br/> 
                            <span style={{ fontSize: '0.75rem', fontWeight: 'bold', opacity: 0.8 }}>
                                {cleanRole}
                            </span>
                        </Typography>

                        {!estaBloqueado && (
                            <Tooltip title="Mi Perfil">
                                <IconButton color="inherit" onClick={() => navigate('/perfil')}>
                                    <AccountCircleIcon />
                                </IconButton>
                            </Tooltip>
                        )}

                        <Button 
                            color="inherit" 
                            onClick={handleLogout} 
                            startIcon={<ExitToAppIcon />}
                            sx={{ display: { xs: 'none', sm: 'flex' } }}
                        >
                            Salir
                        </Button>

                        <Tooltip title="Cerrar Sesión">
                            <IconButton color="inherit" onClick={handleLogout} sx={{ display: { xs: 'flex', sm: 'none' } }}>
                                <ExitToAppIcon />
                            </IconButton>
                        </Tooltip>
                    </Box>
                </Toolbar>
            </AppBar>

            {user && !estaBloqueado && (
                <Drawer variant="permanent" sx={{ width: drawerWidth, flexShrink: 0, '& .MuiDrawer-paper': { width: drawerWidth, backgroundColor: '#ffffff', borderRight: '1px solid #e0e0e0', borderRadius: '0 !important' } }}>
                    <Toolbar sx={{ minHeight: { xs: '80px', sm: '95px' } }} /> 
                    <List sx={{ pt: 2 }}>
                        {(user?.vistasPermitidas || []).map((vista, index) => {
                            const IconoDinamico = iconMap[vista.icono] || DescriptionIcon;
                            return (
                                <ListItem key={index} disablePadding>
                                    <Tooltip title={vista.nombre} placement="right">
                                        <ListItemButton onClick={() => navigate(vista.ruta)} sx={{ justifyContent: 'center' }}>
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

            <Box component="main" sx={{ flexGrow: 1, p: 3, mt: { xs: '80px', sm: '95px' }, width: '100%', ml: `${drawerWidth}px` }}> {/* <-- MARGEN SUPERIOR AJUSTADO */}
                {children}
            </Box>
        </Box>
    );
}