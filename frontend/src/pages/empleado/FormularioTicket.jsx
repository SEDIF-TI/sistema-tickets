import { useState, useContext } from 'react';
import { Box, TextField, Button, Typography, Paper, Alert } from '@mui/material';
import api from '../../services/api'; 
import { AuthContext } from '../../context/AuthContext.jsx';

export default function FormularioTicket() {
    const { user } = useContext(AuthContext); // Obtenemos el usuario para verificar su rol
    
    // Añadimos el campo sede al estado inicial
    const [formData, setFormData] = useState({ titulo: '', sede: '', descripcion: '' });
    const [mensaje, setMensaje] = useState({ texto: '', tipo: '' });

    // Verificamos si el usuario pertenece a soporte (ajuste la condición según los roles exactos en su base de datos)
    const esSoporte = user?.rol === 'SOPORTE' || user?.rol === 'ADMIN';

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            await api.post('/v1/tickets', formData);
            setMensaje({ texto: 'Ticket creado exitosamente.', tipo: 'success' });
            setFormData({ titulo: '', sede: '', descripcion: '' });
        } catch (error) {
            setMensaje({ texto: 'Error al crear el ticket.', tipo: 'error' });
        }
    };

    return (
        <Paper sx={{ p: 3 }}>
            <Typography variant="h6">Levantar Nuevo Ticket</Typography>
            {mensaje.texto && <Alert severity={mensaje.tipo} sx={{ mt: 2 }}>{mensaje.texto}</Alert>}
            
            <Box component="form" onSubmit={handleSubmit} sx={{ mt: 2 }}>
                <TextField 
                    fullWidth 
                    label="Título" 
                    value={formData.titulo} 
                    onChange={(e) => setFormData({...formData, titulo: e.target.value})} 
                    margin="normal" 
                    required 
                />
                
                {/* Renderización condicional del campo Sede solo para Soporte */}
                {esSoporte && (
                    <TextField 
                        fullWidth 
                        label="Sede" 
                        value={formData.sede} 
                        onChange={(e) => setFormData({...formData, sede: e.target.value})} 
                        margin="normal" 
                        required={esSoporte} 
                    />
                )}

                <TextField 
                    fullWidth 
                    label="Descripción" 
                    value={formData.descripcion} 
                    onChange={(e) => setFormData({...formData, descripcion: e.target.value})} 
                    margin="normal" 
                    multiline 
                    rows={4} 
                    required 
                />
                
                <Button type="submit" variant="contained" sx={{ mt: 2 }}>
                    Enviar Ticket
                </Button>
            </Box>
        </Paper>
    );
}