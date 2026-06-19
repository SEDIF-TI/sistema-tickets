import React, { useState, useContext } from 'react';
import { Box, Typography, Paper, TextField, Button, Alert, Divider, Grid } from '@mui/material';
import { useNavigate } from 'react-router-dom'; // <-- 1. IMPORTAMOS EL NAVEGADOR
import { AuthContext } from '../context/AuthContext';
import api from '../services/api';

// Iconos
import LockResetIcon from '@mui/icons-material/LockReset';
import TelegramIcon from '@mui/icons-material/Telegram';
import NotificationsActiveIcon from '@mui/icons-material/NotificationsActive';

const COLOR_GUINDA = '#801A36';

export default function PerfilPage() {
    const { user, marcarPasswordCambiada } = useContext(AuthContext);
    const navigate = useNavigate(); // <-- 2. INICIALIZAMOS EL NAVEGADOR
    
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [mensaje, setMensaje] = useState({ texto: '', tipo: '' });

    // Obtener el rol para condicionar la vista de Telegram
    const userRole = user?.rol || user?.role || user?.rolNombre || '';
    const cleanRole = userRole.replace('ROLE_', '').toUpperCase();
    const mostrarTelegram = cleanRole === 'ADMINISTRADOR' || cleanRole === 'SOPORTE';

    const handleActualizar = async (e) => {
        e.preventDefault();
        setMensaje({ texto: '', tipo: '' });

        if (password !== confirmPassword) {
            setMensaje({ texto: 'Las contraseñas no coinciden.', tipo: 'error' });
            return;
        }

        if (password.length < 6) {
            setMensaje({ texto: 'La contraseña debe tener al menos 6 caracteres.', tipo: 'warning' });
            return;
        }

        try {
            await api.put('/v1/admin/usuarios/password', { nuevaPassword: password });
            
            // Si el usuario venía del bloqueo por contraseña temporal (primer inicio de sesión)
            if (user?.passwordTemporal) {
                marcarPasswordCambiada(); // Rompemos el candado en el contexto
                
                // ¡AQUÍ ESTÁ LA MAGIA! 
                // Los mandamos a la ruta raíz "/" para que App.jsx los lea limpios y los mande a su verdadero Dashboard
                navigate('/', { replace: true });
            } else {
                // Si es un cambio de contraseña normal (desde su sesión ya activa), solo avisamos
                setMensaje({ texto: '¡Contraseña actualizada con éxito!', tipo: 'success' });
                setPassword('');
                setConfirmPassword('');
            }
            
        } catch (error) {
            setMensaje({ texto: 'Ocurrió un error al actualizar la contraseña.', tipo: 'error' });
        }
    };

    const handleVincularTelegram = () => {
        const urlBotTelegram = `https://t.me/SEDIF_Soporte_Bot?start=${user?.correo}`;
        window.open(urlBotTelegram, '_blank');
    };

    return (
        <Box sx={{ p: 3, maxWidth: mostrarTelegram ? 900 : 500, mx: 'auto' }}>
            {user?.passwordTemporal && (
                <Alert severity="warning" variant="filled" sx={{ mb: 4, fontWeight: 'bold', fontSize: '1.1rem' }}>
                    ¡Atención! Por políticas de seguridad, debes cambiar tu contraseña temporal antes de acceder al sistema.
                </Alert>
            )}

            <Typography variant="h4" sx={{ mb: 3, fontWeight: 'bold', color: COLOR_GUINDA }}>
                Mi Perfil y Configuración
            </Typography>

            <Grid container spacing={4}>
                {/* COLUMNA IZQUIERDA: Cambio de Contraseña */}
                <Grid item xs={12} md={mostrarTelegram ? 6 : 12}>
                    <Paper elevation={3} sx={{ p: 4, borderRadius: 2, height: '100%' }}>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2, color: COLOR_GUINDA }}>
                            <LockResetIcon fontSize="large" />
                            <Typography variant="h6" fontWeight="bold">Credenciales</Typography>
                        </Box>
                        <Divider sx={{ mb: 3 }} />

                        {mensaje.texto && (
                            <Alert severity={mensaje.tipo} sx={{ mb: 3 }}>{mensaje.texto}</Alert>
                        )}

                        <Box component="form" onSubmit={handleActualizar}>
                            <TextField label="Nueva Contraseña" type="password" fullWidth margin="normal" value={password} onChange={(e) => setPassword(e.target.value)} required />
                            <TextField label="Confirmar Nueva Contraseña" type="password" fullWidth margin="normal" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} required />
                            <Button type="submit" variant="contained" fullWidth sx={{ mt: 3, py: 1.5, bgcolor: COLOR_GUINDA, '&:hover': { bgcolor: '#5e1026' } }}>
                                {user?.passwordTemporal ? "Establecer y Desbloquear Sistema" : "Actualizar Contraseña"}
                            </Button>
                        </Box>
                    </Paper>
                </Grid>

                {/* COLUMNA DERECHA: Telegram (Solo visible para Admin o Soporte) */}
                {mostrarTelegram && (
                    <Grid item xs={12} md={6}>
                        {!user?.passwordTemporal ? (
                            <Paper elevation={3} sx={{ p: 4, borderRadius: 2, height: '100%', display: 'flex', flexDirection: 'column' }}>
                                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2, color: '#0088cc' }}>
                                    <NotificationsActiveIcon fontSize="large" />
                                    <Typography variant="h6" fontWeight="bold">Notificaciones</Typography>
                                </Box>
                                <Divider sx={{ mb: 3 }} />
                                
                                <Typography variant="body1" color="textSecondary" sx={{ mb: 4, flexGrow: 1 }}>
                                    Vincula tu cuenta con nuestro Bot de Telegram para recibir alertas en tiempo real cuando te asignen un ticket, haya actualizaciones o recibas comunicados.
                                </Typography>

                                <Button onClick={handleVincularTelegram} variant="contained" startIcon={<TelegramIcon />} fullWidth sx={{ py: 1.5, bgcolor: '#0088cc', '&:hover': { bgcolor: '#0077b5' }, fontSize: '1.1rem' }}>
                                    Vincular con Telegram
                                </Button>
                            </Paper>
                        ) : (
                            <Paper elevation={0} sx={{ p: 4, borderRadius: 2, height: '100%', bgcolor: '#f5f5f5', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                <Typography variant="body2" color="textSecondary" align="center">
                                    Las opciones de notificaciones se habilitarán en cuanto establezcas tu nueva contraseña segura.
                                </Typography>
                            </Paper>
                        )}
                    </Grid>
                )}
            </Grid>
        </Box>
    );
}