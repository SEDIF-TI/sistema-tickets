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
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        <Typography variant="body2">
                            {user?.nombre} ({user?.rol})
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
                </List>
            </Drawer>

            <Box component="main" sx={{ flexGrow: 1, p: 3, mt: 8 }}>
                {children}
            </Box>
        </Box>
    );
}