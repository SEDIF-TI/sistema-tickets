import React, { useState, useEffect } from 'react';
import { 
    Box, Typography, Paper, TextField, Button, Table, TableBody, 
    TableCell, TableContainer, TableHead, TableRow, 
    Chip, Switch, Tooltip, IconButton, Autocomplete, 
    Snackbar, Alert // <--- Agregamos los componentes para la notificación
} from '@mui/material';

import CampaignIcon from '@mui/icons-material/Campaign';
import PostAddIcon from '@mui/icons-material/PostAdd';
import DeleteIcon from '@mui/icons-material/Delete';

import api from '../../services/api';
import { areaService } from '../../services/areaService'; 

const COLOR_GUINDA = '#801A36'; 

export default function AdminAvisosPage() {
    const [avisos, setAvisos] = useState([]);
    const [areas, setAreas] = useState([]); 
    const [titulo, setTitulo] = useState('');
    const [mensaje, setMensaje] = useState('');
    const [areaDestino, setAreaDestino] = useState(null); 
    const [enviando, setEnviando] = useState(false);

    // ---> ESTADO PARA EL MENSAJE DE ÉXITO <---
    const [notificacion, setNotificacion] = useState({ open: false, mensaje: '', tipo: 'success' });

    const cargarDatos = async () => {
        try {
            const [resAvisos, resAreas] = await Promise.all([
                api.get('/v1/avisos'),
                areaService.getAll() 
            ]);
            setAvisos(resAvisos.data);
            setAreas(resAreas.data);
        } catch (error) {
            console.error("Error al cargar datos:", error);
        }
    };

    useEffect(() => { cargarDatos(); }, []);

    // Función para cerrar la notificación flotante
    const handleCloseNotificacion = (event, reason) => {
        if (reason === 'clickaway') return;
        setNotificacion({ ...notificacion, open: false });
    };

    const handlePublicar = async (e) => {
        e.preventDefault();
        if (!titulo || !mensaje) return alert("Título y mensaje son obligatorios");
        
        setEnviando(true);
        try {
            await api.post('/v1/avisos', { 
                titulo, 
                mensaje, 
                activo: true, 
                areaId: areaDestino ? areaDestino.id : null 
            });
            setTitulo(''); setMensaje(''); setAreaDestino(null);
            cargarDatos();
            // ✅ MOSTRAR MENSAJE DE ÉXITO AL CREAR
            setNotificacion({ open: true, mensaje: 'Aviso creado y publicado correctamente.', tipo: 'success' });
        } catch (error) { 
            setNotificacion({ open: true, mensaje: 'Error al guardar el aviso.', tipo: 'error' });
        } finally { setEnviando(false); }
    };

    const handleToggleEstado = async (aviso, isChecked) => {
        try {
            await api.put(`/v1/avisos/${aviso.id}`, { 
                ...aviso,
                activo: isChecked
            });
            cargarDatos(); 
            // ✅ MOSTRAR MENSAJE DE ÉXITO AL ENCENDER/APAGAR
            setNotificacion({ 
                open: true, 
                mensaje: isChecked ? 'Aviso encendido en pantallas.' : 'Aviso apagado.', 
                tipo: 'info' 
            });
        } catch (error) { 
            setNotificacion({ open: true, mensaje: 'Error al actualizar estado.', tipo: 'error' });
        }
    };

    const handleEliminar = async (id) => {
        if (window.confirm('¿Eliminar definitivamente?')) {
            try {
                await api.delete(`/v1/avisos/${id}`); 
                cargarDatos();
                // ✅ MOSTRAR MENSAJE DE ÉXITO AL ELIMINAR
                setNotificacion({ open: true, mensaje: 'Aviso eliminado de la base de datos.', tipo: 'success' });
            } catch (error) {
                setNotificacion({ open: true, mensaje: 'Error al eliminar.', tipo: 'error' });
            }
        }
    };

    return (
        <Box sx={{ p: 3, width: '100%' }}>
            
            {/* ---> COMPONENTE SNACKBAR FLOTANTE <--- */}
            <Snackbar 
                open={notificacion.open} 
                autoHideDuration={4000} // Desaparece a los 4 segundos
                onClose={handleCloseNotificacion} 
                anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }} // Sale abajo a la derecha
            >
                <Alert onClose={handleCloseNotificacion} severity={notificacion.tipo} sx={{ width: '100%', fontWeight: 'bold', boxShadow: 3 }}>
                    {notificacion.mensaje}
                </Alert>
            </Snackbar>

            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 3 }}>
                <CampaignIcon sx={{ fontSize: 35, color: COLOR_GUINDA }} />
                <Typography variant="h4" fontWeight="bold" sx={{ color: COLOR_GUINDA }}>Gestión de Avisos</Typography>
            </Box>

            <Paper elevation={2} sx={{ p: 3, borderRadius: 2, mb: 3 }}>
                <Typography variant="h6" sx={{ mb: 2, fontWeight: 'bold' }}>Crear Nuevo Aviso</Typography>
                <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'flex-start' }}>
                    <TextField label="Título" sx={{ flex: 1, minWidth: '200px' }} value={titulo} onChange={(e) => setTitulo(e.target.value)} />
                    <TextField label="Mensaje" sx={{ flex: 2, minWidth: '300px' }} value={mensaje} onChange={(e) => setMensaje(e.target.value)} />
                    
                    <Autocomplete
                        options={areas}
                        getOptionLabel={(option) => option.nombre || ''}
                        value={areaDestino}
                        onChange={(event, newValue) => setAreaDestino(newValue)}
                        sx={{ flex: 1, minWidth: '200px' }}
                        renderInput={(params) => <TextField {...params} label="Área (Vacío = Global)" />}
                    />

                    <Button variant="contained" onClick={handlePublicar} sx={{ bgcolor: COLOR_GUINDA, height: '56px', px: 4 }}>
                        {enviando ? 'Guardando...' : 'Publicar'}
                    </Button>
                </Box>
            </Paper>

            <TableContainer component={Paper} sx={{ borderRadius: 2 }}>
                <Table>
                    <TableHead sx={{ bgcolor: '#f8fafc' }}>
                        <TableRow>
                            <TableCell>Título</TableCell>
                            <TableCell>Mensaje</TableCell>
                            <TableCell>Alcance</TableCell>
                            <TableCell align="center">Estado</TableCell>
                            <TableCell align="center">Acciones</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {avisos.map((aviso) => (
                            <TableRow key={aviso.id} hover>
                                <TableCell sx={{ fontWeight: 'bold' }}>{aviso.titulo}</TableCell>
                                <TableCell>{aviso.mensaje}</TableCell>
                                <TableCell>
                                    <Chip 
                                        label={aviso.areaId ? areas.find(a => a.id === aviso.areaId)?.nombre || 'Área específica' : 'GLOBAL'} 
                                        color={aviso.areaId ? "primary" : "secondary"}
                                        size="small"
                                        variant="outlined"
                                    />
                                </TableCell>
                                <TableCell align="center">
                                    <Chip label={aviso.activo ? "ACTIVO" : "INACTIVO"} color={aviso.activo ? "success" : "default"} size="small" />
                                </TableCell>
                                <TableCell align="center">
                                    <Switch checked={aviso.activo} onChange={(e) => handleToggleEstado(aviso, e.target.checked)} color="success" />
                                    <IconButton color="error" onClick={() => handleEliminar(aviso.id)}><DeleteIcon /></IconButton>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
        </Box>
    );
}