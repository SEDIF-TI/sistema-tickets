import { useState } from 'react';
import { Box, TextField, Button, Typography, Paper, Alert } from '@mui/material';
import api from '../../services/api'; 

export default function FormularioTicket() {
    const [formData, setFormData] = useState({ titulo: '', descripcion: '' });
    const [mensaje, setMensaje] = useState({ texto: '', tipo: '' });

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            await api.post('/v1/tickets', formData);
            setMensaje({ texto: 'Ticket creado exitosamente.', tipo: 'success' });
            setFormData({ titulo: '', descripcion: '' });
        } catch (error) {
            setMensaje({ texto: 'Error al crear el ticket.', tipo: 'error' });
        }
    };

    return (
        <Paper sx={{ p: 3 }}>
            <Typography variant="h6">Levantar Nuevo Ticket</Typography>
            {mensaje.texto && <Alert severity={mensaje.tipo} sx={{ mt: 2 }}>{mensaje.texto}</Alert>}
            <Box component="form" onSubmit={handleSubmit} sx={{ mt: 2 }}>
                <TextField fullWidth label="Título" value={formData.titulo} 
                    onChange={(e) => setFormData({...formData, titulo: e.target.value})} margin="normal" required />
                <TextField fullWidth label="Descripción" value={formData.descripcion} 
                    onChange={(e) => setFormData({...formData, descripcion: e.target.value})} margin="normal" multiline rows={4} required />
                <Button type="submit" variant="contained" sx={{ mt: 2 }}>Enviar Ticket</Button>
            </Box>
        </Paper>
    );
}