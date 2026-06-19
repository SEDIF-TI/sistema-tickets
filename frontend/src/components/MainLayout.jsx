import { useContext, useEffect, useState } from 'react';
import { AuthContext } from '../context/AuthContext.jsx';
import { getIcon } from '../util/iconMapper.js';
import { useNavigate } from 'react-router-dom';
import { 
    Box, 
    Drawer, 
    AppBar, 
    Toolbar, 
    List, 
    Typography, 
    ListItem, 
    ListItemButton, 
    ListItemIcon, 
    Tooltip,
    Divider,
    Alert,
    AlertTitle
} from '@mui/material';

// Imports de iconos corregidos (sin duplicados)
import LogoutIcon from '@mui/icons-material/Logout';
import api from '../services/api.js';
import InfoIcon from '@mui/icons-material/Info';

const drawerWidth = 240;

export default function MainLayout({ children }) {
    const { user, logout } = useContext(AuthContext);
    const navigate = useNavigate();

    // --- 1. LÓGICA DE AVISOS GLOBALES ---
    const [avisos, setAvisos] = useState([]);

    useEffect(() => {
        // Consultamos los avisos activos al cargar la pantalla
        const fetchAvisos = async () => {
            try {
                const respuesta = await api.get('/v1/avisos/activos');
                setAvisos(respuesta.data);
            } catch (error) {
                console.error("Error cargando avisos globales:", error);
            }
        };
        
        // Solo intentamos cargar si hay un usuario logueado
        if (user) {
            fetchAvisos();
        }
    }, [user]);
    // ------------------------------------

    // Ahora las vistas vienen dinámicamente desde la base de datos
    const vistas = user?.vistas || [];

    return (
        <Box sx={{ display: 'flex' }}>
            <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}>
                <Toolbar sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="h6" noWrap component="div" sx={{ fontWeight: 'bold' }}>
                        SEDIF - Sistema de Tickets
                    </Typography>
                    <LogoutIcon onClick={() => { logout(); navigate('/login'); }} sx={{ cursor: 'pointer' }} />
                </Toolbar>
            </AppBar>

            <Drawer
                variant="permanent"
                sx={{
                    width: drawerWidth,
                    flexShrink: 0,
                    [`& .MuiDrawer-paper`]: { width: drawerWidth, boxSizing: 'border-box', mt: 8 },
                }}
            >
                <List>
                    {vistas.map((vista, index) => {
                        // LA MAGIA: Si getIcon falla, usa el icono de interrogación
                        const Icono = getIcon(vista.icono) || InfoIcon;
                        
                        return (
                            <ListItem key={index} disablePadding sx={{ display: 'block' }}>
                                <Tooltip title={vista.nombre} placement="right">
                                    <ListItemButton 
                                        onClick={() => navigate(vista.ruta)} 
                                        sx={{ justifyContent: 'center', py: 2 }}
                                    >
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

            <Box component="main" sx={{ flexGrow: 1, p: 3, mt: 8 }}>
                
                {/* --- 2. SECCIÓN VISUAL DE AVISOS PARA TODOS --- */}
                {/* Se mostrará justo arriba del contenido principal si hay avisos */}
                {avisos.length > 0 && (
                    <Box sx={{ mb: 3 }}>
                        {avisos.map(aviso => (
                            <Alert severity="info" key={aviso.id} sx={{ mb: 1, borderRadius: 2 }}>
                                <AlertTitle sx={{ fontWeight: 'bold' }}>{aviso.titulo}</AlertTitle>
                                {aviso.mensaje}
                            </Alert>
                        ))}
                    </Box>
                )}
                {/* ---------------------------------------------- */}

                {children}
            </Box>
        </Box>
    );
}