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
    Tooltip,
    Divider
} from '@mui/material';
import LogoutIcon from '@mui/icons-material/Logout';

const drawerWidth = 240;

export default function MainLayout({ children }) {
    const { user, logout } = useContext(AuthContext);
    const navigate = useNavigate();

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
                        const Icono = getIcon(vista.icono);
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
                {children}
            </Box>
        </Box>
    );
}