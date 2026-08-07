import { Dialog, DialogTitle, DialogContent, DialogActions, Button, TextField, Box, MenuItem } from '@mui/material';
import { useState } from 'react';
import api from '../services/api';
import { toUpper } from '../util/formater';

/**
 * Alta rápida de un ticket desde un diálogo.
 *
 * El formulario mantiene su propio estado en un único objeto y transforma a
 * mayúsculas el título y la descripción al escribir, criterio del sistema para
 * los textos que acaban en documentos. Tras crear el ticket avisa a la pantalla
 * con `onTicketCreated` para que recargue su listado, y se cierra.
 */
export default function TicketModal({ open, handleClose, onTicketCreated }) {
    const [formData, setFormData] = useState({ titulo: '', descripcion: '', categoriaId: '', prioridad: 'BAJA' });

    const handleSubmit = async () => {
        try {
            await api.post('/v1/tickets', formData);
            onTicketCreated();
            handleClose();
        } catch (error) {
            console.error("Error al crear ticket:", error);
        }
    };

    return (
        <Dialog open={open} onClose={handleClose} fullWidth maxWidth="sm">
            <DialogTitle>Crear Nuevo Ticket</DialogTitle>
            <DialogContent>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
                    <TextField label="Título" fullWidth onChange={(e) => setFormData({...formData, titulo: toUpper(e.target.value)})} />
                    <TextField label="Descripción" fullWidth multiline rows={4} onChange={(e) => setFormData({...formData, descripcion: toUpper(e.target.value)})} />
                    <TextField select label="Prioridad" fullWidth value={formData.prioridad} onChange={(e) => setFormData({...formData, prioridad: e.target.value})}>
                        <MenuItem value="BAJA">Baja</MenuItem>
                        <MenuItem value="MEDIA">Media</MenuItem>
                        <MenuItem value="ALTA">Alta</MenuItem>
                    </TextField>
                </Box>
            </DialogContent>
            <DialogActions>
                <Button onClick={handleClose}>Cancelar</Button>
                <Button onClick={handleSubmit} variant="contained">Guardar Ticket</Button>
            </DialogActions>
        </Dialog>
    );
}