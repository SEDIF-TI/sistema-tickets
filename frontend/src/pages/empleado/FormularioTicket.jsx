import React, { useState } from 'react';
import { Box, Typography, TextField, Button, Alert, Paper } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import api from '../../services/api';

export default function FormularioTicket() {
    const [titulo, setTitulo] = useState('');
    const [descripcion, setDescripcion] = useState('');
    const [mensaje, setMensaje] = useState({ tipo: '', texto: '' });
    
    // Hook para redirigir al usuario
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setMensaje({ tipo: '', texto: '' });

        try {
            await api.post('/v1/tickets', { titulo, descripcion });
            
            // AQUÍ ESTÁ LA MAGIA: Enviamos un 'state' oculto junto con la redirección
            navigate('/empleado/historial', { 
                state: { mensajeExito: '¡Ticket creado correctamente!' } 
            });
            
        }catch (error) {
            console.error("Error al crear:", error);
            setMensaje({ tipo: 'error', texto: 'Hubo un error al crear el ticket.' });
        }
    };

    return (
        <Box sx={{ maxWidth: 800, mx: 'auto', p: 3 }}>
            <Paper elevation={2} sx={{ p: 4 }}>
                <Typography variant="h5" gutterBottom>Levantar Nuevo Ticket</Typography>
                
                {mensaje.texto && (
                    <Alert severity={mensaje.tipo} sx={{ mb: 3 }}>
                        {mensaje.texto}
                    </Alert>
                )}

                <form onSubmit={handleSubmit}>
                    <TextField 
                        fullWidth label="Título *" variant="outlined" margin="normal"
                        value={titulo} onChange={(e) => setTitulo(e.target.value)} required 
                    />
                    <TextField 
                        fullWidth label="Descripción *" variant="outlined" margin="normal"
                        multiline rows={4}
                        value={descripcion} onChange={(e) => setDescripcion(e.target.value)} required 
                    />
                    <Box sx={{ mt: 3 }}>
                        <Button type="submit" variant="contained" sx={{ bgcolor: '#5c0a28', '&:hover': { bgcolor: '#4a0820' }, px: 4, py: 1 }}>
                            Enviar Ticket
                        </Button>
                    </Box>
                </form>
            </Paper>
        </Box>
    );
}