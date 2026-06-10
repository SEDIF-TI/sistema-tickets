import { useContext } from 'react';
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
    Button,
    Tooltip
} from '@mui/material';
import LogoutIcon from '@mui/icons-material/Logout';

import AddIcon from '@mui/icons-material/Add';
import ListIcon from '@mui/icons-material/List';
const drawerWidth = 240;

export default function MainLayout({ children }) {
    const { user, logout } = useContext(AuthContext);
    const navigate = useNavigate();

    const vistas = user?.vistas || [];

    return (
        <Box sx={{ display: 'flex' }}>
            <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}>
                <Toolbar sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="h6" noWrap component="div" sx={{ fontWeight: 'bold' }}>
                        SEDIF - Sistema de Tickets
                    </Typography>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: { xs: 1, sm: 2 } }}>
                        <Typography variant="body2" sx={{ display: { xs: 'none', sm: 'block' } }}>
                            {user?.nombre} ({user?.rol})
                        </Typography>
                        
                        {/* NUEVO BOTÓN DE TELEGRAM */}
                        <Button 
                            variant="outlined"
                            color="inherit" 
                            href={`https://t.me/Notificaciones_SEDIF_bot?start=${user?.usuarioId}`}
                            target="_blank" // Para que abra Telegram en una pestaña nueva
                            sx={{ textTransform: 'none', borderColor: 'rgba(255,255,255,0.5)', display: { xs: 'none', sm: 'flex' } }}
                        >
                            Vincular Telegram
                        </Button>

                        <Button 
                            color="inherit" 
                            startIcon={<LogoutIcon />} 
                            onClick={() => { logout(); navigate('/login'); }}
                            sx={{ textTransform: 'none' }}
                        >
                            Salir
                        </Button>
                    </Box>
                </Toolbar>
            </AppBar>

            <Drawer
                variant="permanent"
                sx={{
                    width: 70,
                    flexShrink: 0,
                    [`& .MuiDrawer-paper`]: { width: 70, overflowX: 'hidden' },
                }}
            >
                <Toolbar />
                <List>
                    {vistas.map((vista, index) => {
                        const Icono = getIcon(vista.icono);
                        return (
                            <ListItem key={index} disablePadding sx={{ display: 'block' }}>
                                <Tooltip title={vista.nombre || 'Menú'} placement="right">
                                    <ListItemButton onClick={() => navigate(vista.ruta)} sx={{ justifyContent: 'center', py: 2 }}>
                                        <ListItemIcon sx={{ minWidth: 0, justifyContent: 'center' }}>
                                            <Icono color="primary" />
                                        </ListItemIcon>
                                    </ListItemButton>
                                </Tooltip>
                            </ListItem>
                        );
                    })}
                    {/* Botones Fijos para Empleado */}
                    {/* Botones Fijos para Empleado */}
                    {/* Botones Fijos para Empleado */}
                    <ListItem disablePadding sx={{ display: 'block' }}>
                        <ListItemButton onClick={() => navigate('/empleado/nuevo')} sx={{ justifyContent: 'center', py: 2 }}>
                            <ListItemIcon sx={{ minWidth: 0, justifyContent: 'center' }}>
                                <AddIcon sx={{ color: '#5c0a28' }} /> {/* <-- CAMBIO AQUÍ */}
                            </ListItemIcon>
                        </ListItemButton>
                    </ListItem>

                    <ListItem disablePadding sx={{ display: 'block' }}>
                        <ListItemButton onClick={() => navigate('/empleado/historial')} sx={{ justifyContent: 'center', py: 2 }}>
                            <ListItemIcon sx={{ minWidth: 0, justifyContent: 'center' }}>
                                <ListIcon sx={{ color: '#5c0a28' }} /> {/* <-- CAMBIO AQUÍ */}
                            </ListItemIcon>
                        </ListItemButton>
                    </ListItem>
                </List>
            </Drawer>

            <Box component="main" sx={{ flexGrow: 1, p: 3, mt: 8 }}>
                {children}
            </Box>
        </Box>
    );
}