import React, { useState } from 'react';
import { Box, Typography, Paper, TextField, Button, Alert } from '@mui/material';
import api from '../services/api';

export default function PerfilPage() {
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [mensaje, setMensaje] = useState({ texto: '', tipo: '' });

    const handleActualizar = async (e) => {
        e.preventDefault();
        setMensaje({ texto: '', tipo: '' });

        if (password !== confirmPassword) {
            setMensaje({ texto: 'Las contraseñas no coinciden', tipo: 'error' });
            return;
        }

        if (password.length < 6) {
            setMensaje({ texto: 'La contraseña debe tener al menos 6 caracteres', tipo: 'warning' });
            return;
        }

        try {
            // Asegúrate de que esta ruta coincida con la que liberamos en Spring Security
            await api.put('/v1/admin/usuarios/password', { nuevaPassword: password });
            
            setMensaje({ texto: '¡Contraseña actualizada con éxito!', tipo: 'success' });
            setPassword('');
            setConfirmPassword('');
        } catch (error) {
            setMensaje({ texto: 'Ocurrió un error al actualizar la contraseña.', tipo: 'error' });
        }
    };

    return (
        <Box sx={{ p: 3, maxWidth: 600, mx: 'auto' }}>
            <Typography variant="h4" sx={{ mb: 3, fontWeight: 'bold', color: '#2c3e50' }}>
                Mi Perfil
            </Typography>

            <Paper elevation={3} sx={{ p: 4, borderRadius: 2 }}>
                <Typography variant="h6" sx={{ mb: 2 }}>
                    Cambiar Contraseña
                </Typography>

                {mensaje.texto && (
                    <Alert severity={mensaje.tipo} sx={{ mb: 3 }}>
                        {mensaje.texto}
                    </Alert>
                )}

                <Box component="form" onSubmit={handleActualizar}>
                    <TextField
                        label="Nueva Contraseña"
                        type="password"
                        fullWidth
                        margin="normal"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                    />
                    <TextField
                        label="Confirmar Nueva Contraseña"
                        type="password"
                        fullWidth
                        margin="normal"
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        required
                    />
                    <Button 
                        type="submit" 
                        variant="contained" 
                        color="primary" 
                        fullWidth 
                        sx={{ mt: 3, py: 1.5 }}
                    >
                        Actualizar Contraseña
                    </Button>
                </Box>
            </Paper>
        </Box>
    );
}