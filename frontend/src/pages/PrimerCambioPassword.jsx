import React, { useState, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, Paper, Typography, TextField, Button, Alert } from '@mui/material';
import { AuthContext } from '../context/AuthContext';
import api from '../services/api';

export default function PrimerCambioPassword() {
    // 1. Extraemos la función correcta de nuestro contexto
    const { marcarPasswordCambiada } = useContext(AuthContext);
    const navigate = useNavigate();

    const [nuevaPassword, setNuevaPassword] = useState('');
    const [confirmarPassword, setConfirmarPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const handleActualizar = async (e) => {
        e.preventDefault();
        setError('');

        if (nuevaPassword.length < 6) {
            setError('La contraseña debe tener al menos 6 caracteres.');
            return;
        }

        if (nuevaPassword !== confirmarPassword) {
            setError('Las contraseñas no coinciden.');
            return;
        }

        setLoading(true);
        try {
            // Llamamos a tu endpoint existente
            await api.put('/v1/admin/usuarios/password', { nuevaPassword });
            
            alert('Contraseña actualizada con éxito. ¡Bienvenido al sistema!');
            
            // 2. ¡La magia en acción! Rompemos el bloqueo y redirigimos
            marcarPasswordCambiada(); 
            navigate('/'); 
            
        } catch (err) {
            setError(err.response?.data || 'No se pudo actualizar la contraseña.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <Box sx={{ display: 'flex', height: '100vh', alignItems: 'center', justifyContent: 'center', bgcolor: '#f5f5f5' }}>
            <Paper sx={{ p: 4, maxWidth: 400, width: '100%', textAlign: 'center' }} elevation={3}>
                <Typography variant="h5" fontWeight="bold" color="#5c0a28" mb={1}>
                    Primer Inicio de Sesión
                </Typography>
                <Typography variant="body2" color="textSecondary" mb={3}>
                    Por seguridad, debes cambiar la contraseña temporal asignada antes de continuar.
                </Typography>

                {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

                <form onSubmit={handleActualizar}>
                    <TextField
                        label="Nueva Contraseña"
                        type="password"
                        fullWidth
                        required
                        margin="dense"
                        value={nuevaPassword}
                        onChange={(e) => setNuevaPassword(e.target.value)}
                    />
                    <TextField
                        label="Confirmar Nueva Contraseña"
                        type="password"
                        fullWidth
                        required
                        margin="dense"
                        value={confirmarPassword}
                        onChange={(e) => setConfirmarPassword(e.target.value)}
                    />
                    <Button 
                        type="submit" 
                        variant="contained" 
                        fullWidth 
                        sx={{ mt: 3, bgcolor: '#5c0a28', '&:hover': { bgcolor: '#42071c' } }}
                        disabled={loading}
                    >
                        {loading ? 'Actualizando...' : 'Actualizar y Entrar'}
                    </Button>
                </form>
            </Paper>
        </Box>
    );
}