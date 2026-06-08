import { useContext, useState } from 'react';
import { AuthContext } from '../context/AuthContext.jsx';
import { 
    Box, Drawer, AppBar, Toolbar, List, Typography, Divider, 
    IconButton, ListItem, ListItemButton, ListItemIcon, ListItemText, Button,
    Badge, Snackbar, Alert 
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import AssignmentIcon from '@mui/icons-material/Assignment';
import LogoutIcon from '@mui/icons-material/Logout';
import AddCircleIcon from '@mui/icons-material/AddCircle';
import NotificationsIcon from '@mui/icons-material/Notifications';

const drawerWidth = 240;

export default function SoporteLayout({ children }) {
    const { user, logout } = useContext(AuthContext);
    
    // Estados para controlar la notificación central en pantalla
    const [openAviso, setOpenAviso] = useState(false);
    const [mensajeAviso, setMensajeAviso] = useState('');

    // Función de prueba para simular la llegada de un aviso (luego lo conectaremos a WebSockets)
    const simularLlegadaAviso = () => {
        setMensajeAviso("¡Atención! Ha llegado un nuevo Aviso Global del administrador.");
        setOpenAviso(true);
    };

    const handleCloseAviso = () => {
        setOpenAviso(false);
    };

    const drawer = (
        <div>
            <Toolbar sx={{ backgroundColor: 'primary.main', color: 'white' }}>
                <Typography variant="h6" noWrap component="div">
                    SEDIF Tickets
                </Typography>
            </Toolbar>
            <Divider />
            <List>
                <ListItem disablePadding>
                    <ListItemButton>
                        <ListItemIcon><AssignmentIcon color="primary" /></ListItemIcon>
                        <ListItemText primary="Mis Tickets" />
                    </ListItemButton>
                </ListItem>
                <ListItem disablePadding>
                    <ListItemButton>
                        <ListItemIcon><AddCircleIcon color="primary" /></ListItemIcon>
                        <ListItemText primary="Levantar Ticket" />
                    </ListItemButton>
                </ListItem>
                {/* Se eliminó la opción de Avisos Globales de aquí según tu indicación */}
            </List>
        </div>
    );

    return (
        <Box sx={{ display: 'flex' }}>
            <AppBar position="fixed" sx={{ width: { sm: `calc(100% - ${drawerWidth}px)` }, ml: { sm: `${drawerWidth}px` } }}>
                <Toolbar>
                    <IconButton color="inherit" edge="start" sx={{ mr: 2, display: { sm: 'none' } }}>
                        <MenuIcon />
                    </IconButton>
                    <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1 }}>
                        Panel de Soporte
                    </Typography>

                    {/* Campana de Notificaciones en la parte superior derecha */}
                    <IconButton color="inherit" sx={{ mr: 3 }} onClick={simularLlegadaAviso}>
                        <Badge badgeContent={1} color="error">
                            <NotificationsIcon />
                        </Badge>
                    </IconButton>

                    <Typography variant="body2" sx={{ mr: 2 }}>
                        {user?.nombre || 'Usuario'} | {user?.rol || 'SOPORTE'}
                    </Typography>
                    <Button color="inherit" onClick={logout} startIcon={<LogoutIcon />}>
                        Salir
                    </Button>
                </Toolbar>
            </AppBar>
            <Box component="nav" sx={{ width: { sm: drawerWidth }, flexShrink: { sm: 0 } }}>
                <Drawer variant="permanent" sx={{ display: { xs: 'none', sm: 'block' }, '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth } }} open>
                    {drawer}
                </Drawer>
            </Box>
            <Box component="main" sx={{ flexGrow: 1, p: 3, width: { sm: `calc(100% - ${drawerWidth}px)` } }}>
                <Toolbar />
                {children}
            </Box>

            {/* Componente que muestra la alerta en el centro superior de la pantalla */}
            {/* Busque este bloque al final del archivo SoporteLayout.jsx */}
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
                        minWidth: '400px',        // Ancho mínimo más grande
                        padding: '20px 30px',     // Más espacio interno (arriba/abajo y lados)
                        fontSize: '1.2rem',       // Letra más grande
                        mt: 6, 
                        fontWeight: 'bold',
                        backgroundColor: 'primary.main', 
                        color: 'white',
                        '& .MuiAlert-icon': { 
                            color: 'white',
                            fontSize: '2rem',     // Icono más grande para acompañar el texto
                            mr: 2                 // Más separación entre el icono y el texto
                        } 
                    }}
                >
                    {mensajeAviso}
                </Alert>
            </Snackbar>
        </Box>
    );
}