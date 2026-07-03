import React, { useState, useEffect } from 'react';
import { Box, Typography, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Button, TextField, Dialog, DialogTitle, DialogContent, DialogActions, IconButton, InputAdornment } from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import ComputerIcon from '@mui/icons-material/Computer';
import SearchIcon from '@mui/icons-material/Search';
import api from '../../services/api';
import { toUpper } from '../../util/formater.js';

const COLOR_GUINDA = '#801A36';

export default function AdminEquiposPage() {
    const [equipos, setEquipos] = useState([]);
    const [busqueda, setBusqueda] = useState('');
    const [open, setOpen] = useState(false);
    const [equipoActual, setEquipoActual] = useState({ id: null, descripcion: '', marca: '', modelo: '' });

    const cargarEquipos = async () => {
        try {
            const res = await api.get('/v1/equipos');
            setEquipos(res.data);
        } catch (error) {
            console.error("Error al cargar equipos:", error);
        }
    };

    useEffect(() => { cargarEquipos(); }, []);

    const handleChange = (e) => setEquipoActual({ ...equipoActual, [e.target.name]: toUpper(e.target.value) });

    const handleGuardar = async () => {
        try {
            if (equipoActual.id) {
                await api.put(`/api/v1/equipos/${equipoActual.id}`, equipoActual);
            } else {
                await api.post('/v1/equipos/upsert', equipoActual);
            }
            setOpen(false);
            cargarEquipos();
        } catch (error) {
            // Ahora capturaremos el mensaje real que manda Java
            const mensajeBackend = error.response?.data?.message || error.response?.data || error.message;
            console.error("Error completo del backend:", error.response);
            alert(`El servidor rechazó la petición. Motivo real: ${mensajeBackend}`);
        }
    };

    const handleEliminar = async (id) => {
        if (window.confirm('¿Seguro que deseas eliminar este equipo del catálogo?')) {
            try {
                await api.delete(`/v1/equipos/${id}`);
                cargarEquipos();
            } catch (error) {
                alert("Error al eliminar el equipo.");
            }
        }
    };

    const equiposFiltrados = equipos.filter(eq => 
        (eq.descripcion || '').toLowerCase().includes(busqueda.toLowerCase()) ||
        (eq.marca || '').toLowerCase().includes(busqueda.toLowerCase())
    );

    return (
        <Box sx={{ p: 3, maxWidth: 1200, mx: 'auto' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
                <Typography variant="h4" fontWeight="bold" sx={{ color: COLOR_GUINDA, display: 'flex', alignItems: 'center', gap: 1 }}>
                    <ComputerIcon fontSize="large" /> Catálogo de Equipos
                </Typography>
                <Button variant="contained" onClick={() => { setEquipoActual({ id: null, descripcion: '', marca: '', modelo: '' }); setOpen(true); }} sx={{ bgcolor: COLOR_GUINDA }}>
                    + Nuevo Equipo
                </Button>
            </Box>

            <Paper sx={{ p: 2, mb: 3 }}>
                <TextField fullWidth size="small" placeholder="Buscar por descripción o marca..." value={busqueda} onChange={(e) => setBusqueda(e.target.value)}
                    InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon /></InputAdornment> }}
                />
            </Paper>

            <TableContainer component={Paper}>
                <Table>
                    <TableHead sx={{ bgcolor: '#f8fafc' }}>
                        <TableRow>
                            <TableCell sx={{ fontWeight: 'bold' }}>Descripción</TableCell>
                            <TableCell sx={{ fontWeight: 'bold' }}>Marca</TableCell>
                            <TableCell sx={{ fontWeight: 'bold' }}>Modelo</TableCell>
                            <TableCell align="center" sx={{ fontWeight: 'bold' }}>Acciones</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {equiposFiltrados.map(eq => (
                            <TableRow key={eq.id} hover>
                                <TableCell>{eq.descripcion}</TableCell>
                                <TableCell>{eq.marca}</TableCell>
                                <TableCell>{eq.modelo}</TableCell>
                                <TableCell align="center">
                                    <IconButton color="primary" onClick={() => { setEquipoActual(eq); setOpen(true); }}><EditIcon /></IconButton>
                                    <IconButton color="error" onClick={() => handleEliminar(eq.id)}><DeleteIcon /></IconButton>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>

            <Dialog open={open} onClose={() => setOpen(false)} maxWidth="sm" fullWidth>
                <DialogTitle>{equipoActual.id ? 'Editar Equipo' : 'Nuevo Equipo'}</DialogTitle>
                <DialogContent>
                    <TextField autoFocus margin="dense" label="Descripción (Obligatorio)" name="descripcion" fullWidth value={equipoActual.descripcion} onChange={handleChange} required />
                    <TextField margin="dense" label="Marca" name="marca" fullWidth value={equipoActual.marca} onChange={handleChange} />
                    <TextField margin="dense" label="Modelo" name="modelo" fullWidth value={equipoActual.modelo} onChange={handleChange} />
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setOpen(false)} color="inherit">Cancelar</Button>
                    <Button onClick={handleGuardar} variant="contained" sx={{ bgcolor: COLOR_GUINDA }}>Guardar</Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
}