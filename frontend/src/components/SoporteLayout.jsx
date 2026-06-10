import { useContext, useState } from 'react';
import { AuthContext } from '../context/AuthContext.jsx';
import { useNavigate } from 'react-router-dom';
import { 
    Box, Drawer, AppBar, Toolbar, List, Typography, Divider, 
    IconButton, ListItem, ListItemButton, ListItemIcon, Button,
    Badge, Snackbar, Alert, Tooltip 
} from '@mui/material';
import LogoutIcon from '@mui/icons-material/Logout';
import AssignmentIcon from '@mui/icons-material/Assignment';
import AddCircleIcon from '@mui/icons-material/AddCircle';
import NotificationsIcon from '@mui/icons-material/Notifications';

const drawerWidth = 70; // 1. Ancho del menú

export default function SoporteLayout({ children }) {
    // 2. Herramientas de navegación y sesión
    const { user, logout } = useContext(AuthContext);
    const navigate = useNavigate();
    
    // 3. Estados de la notificación
    const [openAviso, setOpenAviso] = useState(false);
    const [mensajeAviso, setMensajeAviso] = useState('');

    const simularLlegadaAviso = () => {
        setMensajeAviso("¡Atención! Ha llegado un nuevo Aviso Global del administrador.");
        setOpenAviso(true);
    };

    const handleCloseAviso = () => {
        setOpenAviso(false);
    };

    return (
        <Box sx={{ display: 'flex' }}>
            
            {/* 4. BARRA SUPERIOR (AppBar) */}
            <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}>
                <Toolbar sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="h6" noWrap component="div" sx={{ fontWeight: 'bold' }}>
                        SEDIF - Sistema de Tickets
                    </Typography>

                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        {/* Campana */}
                        <IconButton color="inherit" onClick={simularLlegadaAviso}>
                            <Badge badgeContent={1} color="error">
                                <NotificationsIcon />
                            </Badge>
                        </IconButton>

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

            {/* 5. MENÚ LATERAL COLAPSADO (Drawer) */}
            <Drawer 
                variant="permanent" 
                sx={{ 
                    width: drawerWidth, 
                    flexShrink: 0, 
                    [`& .MuiDrawer-paper`]: { width: drawerWidth, overflowX: 'hidden' } 
                }}
            >
                <Toolbar /> {/* Empuja los iconos hacia abajo para que la barra superior no los tape */}
                <Divider />
                <List>
                    {/* Bloque del Botón: Mis Tickets */}
                    <ListItem disablePadding sx={{ display: 'block' }}>
                        <Tooltip title="Mis Tickets" placement="right">
                            <ListItemButton 
                                onClick={() => navigate('/soporte/bandeja')} // Navegación SPA interna segura
                                sx={{ justifyContent: 'center', py: 2 }}
                            >
                                <ListItemIcon sx={{ minWidth: 0, justifyContent: 'center' }}>
                                    <AssignmentIcon color="primary" />
                                </ListItemIcon>
                            </ListItemButton>
                        </Tooltip>
                    </ListItem>
                    
                    {/* Bloque del Botón: Levantar Ticket */}
                    <ListItem disablePadding sx={{ display: 'block' }}>
                        <Tooltip title="Levantar Ticket" placement="right">
                            <ListItemButton 
                                onClick={() => navigate('/soporte/nuevo')} // Mismo mecanismo para evitar recargas de página
                                sx={{ justifyContent: 'center', py: 2 }}
                            >
                                <ListItemIcon sx={{ minWidth: 0, justifyContent: 'center' }}>
                                    <AddCircleIcon color="primary" />
                                </ListItemIcon>
                            </ListItemButton>
                        </Tooltip>
                    </ListItem>
                </List>
            </Drawer>

            {/* 6. CONTENIDO PRINCIPAL (Children) */}
            <Box component="main" sx={{ flexGrow: 1, p: 3, mt: 8 }}>
                {children}
            </Box>

            {/* 7. COMPONENTE DE NOTIFICACIÓN (Snackbar) */}
            <Snackbar 
                open={openAviso} 
                autoHideDuration={6000} 
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