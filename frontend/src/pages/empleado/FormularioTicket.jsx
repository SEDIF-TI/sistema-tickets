import { useState } from 'react';
import { Box, TextField, Button, Typography, Paper, Alert } from '@mui/material';
import api from '../../services/api';

export default function FormularioTicket() {
    const [formData, setFormData] = useState({ titulo: '', descripcion: '' });
    const [mensaje, setMensaje] = useState({ texto: '', tipo: '' });

    const handleSubmit = async (e) => {
        e.preventDefault();
        
        try {
            // Enviamos al backend. El backend calcula la hora y asigna técnico/prioridad
            await api.post('/v1/tickets', formData);
            
            setMensaje({ texto: 'Ticket creado exitosamente.', tipo: 'success' });
            setFormData({ titulo: '', descripcion: '' });
        } catch (error) {
            setMensaje({ texto: 'Error al crear el ticket.', tipo: 'error' });
        }
    };

    return (
        <div style={{ backgroundColor: 'red', padding: '50px' }}>
            <h1>SI VES ESTO, EL FORMULARIO SÍ ESTÁ CARGANDO</h1>
        </div>
    );
}