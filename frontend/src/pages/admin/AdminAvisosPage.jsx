import React, { useState, useEffect } from 'react';
import { 
    Box, Typography, Paper, TextField, Button, Table, TableBody, 
    TableCell, TableContainer, TableHead, TableRow, IconButton, 
    Chip, Alert, CircularProgress 
} from '@mui/material';

import CampaignIcon from '@mui/icons-material/Campaign';
import SendIcon from '@mui/icons-material/Send';
import DeleteIcon from '@mui/icons-material/Delete';
import PostAddIcon from '@mui/icons-material/PostAdd';

import api from '../../services/api';

const COLOR_GUINDA = '#5c0a28'; 

export default function AdminAvisosPage() {
    const [avisos, setAvisos] = useState([]);
    const [titulo, setTitulo] = useState('');
    const [mensaje, setMensaje] = useState('');
    const [cargando, setCargando] = useState(true);
    const [enviando, setEnviando] = useState(false);

    const cargarAvisos = async () => {
        try {
            const respuesta = await api.get('/v1/avisos/activos'); 
            setAvisos(respuesta.data);
        } catch (error) { console.error(error); } 
        finally { setCargando(false); }
    };

    useEffect(() => { cargarAvisos(); }, []);

    const handlePublicar = async (e) => {
        e.preventDefault();
        setEnviando(true);
        try {
            await api.post('/v1/avisos', { titulo, mensaje, estado: 'ACTIVO' });
            setTitulo(''); setMensaje(''); cargarAvisos();
        } catch (error) { alert("Error al publicar"); }
        finally { setEnviando(false); }
    };

// ---> FUNCIÓN PARA ELIMINAR / DESACTIVAR <---
    const handleEliminar = async (id) => {
        if (window.confirm('¿Estás seguro de que deseas eliminar este aviso global? Las pantallas de los usuarios se limpiarán en unos segundos.')) {
            try {
                // Opción A: Borrado físico (La que teníamos)
                //await api.delete(`/v1/avisos/${id}`); 
                
                // Opción B (Si la Opción A te da error en consola, comenta la línea de arriba y descomenta la de abajo):
                await api.put(`/v1/avisos/${id}`, { estado: 'INACTIVO' }); 

                cargarAvisos(); // Refresca la tabla del administrador
            } catch (error) {
                alert("Ocurrió un error al eliminar el aviso. Revisa la consola para más detalles.");
                console.error("Detalle del error:", error);
            }
        }
    };

    return (
        <Box sx={{ p: 3, width: '100%', boxSizing: 'border-box' }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 3 }}>
                <CampaignIcon sx={{ fontSize: 35, color: COLOR_GUINDA }} />
                <Typography variant="h4" fontWeight="bold" sx={{ color: COLOR_GUINDA }}>
                    Gestión de Avisos Globales
                </Typography>
            </Box>

            {/* Formulario */}
            <Paper elevation={2} sx={{ p: 3, borderRadius: 2, mb: 3 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                    <PostAddIcon color="action" />
                    <Typography variant="h6" fontWeight="bold">Publicar Nuevo Aviso</Typography>
                </Box>
                <Box sx={{ display: 'flex', gap: 2, alignItems: 'flex-start' }}>
                    <TextField label="Título" sx={{ flex: 1 }} value={titulo} onChange={(e) => setTitulo(e.target.value)} />
                    <TextField label="Mensaje" sx={{ flex: 2 }} value={mensaje} onChange={(e) => setMensaje(e.target.value)} />
                    <Button variant="contained" onClick={handlePublicar} sx={{ bgcolor: COLOR_GUINDA, height: '56px', px: 4 }}>
                        {enviando ? '...' : 'Publicar'}
                    </Button>
                </Box>
            </Paper>

            {/* Tabla Estirada al 100% */}
            <TableContainer component={Paper} elevation={2} sx={{ borderRadius: 2, width: '100%' }}>
                <Table sx={{ width: '100%', tableLayout: 'fixed' }}>
                    <TableHead sx={{ bgcolor: '#f8fafc' }}>
                        <TableRow>
                            <TableCell sx={{ fontWeight: 'bold', width: '5%' }}>ID</TableCell>
                            <TableCell sx={{ fontWeight: 'bold', width: '20%' }}>Título</TableCell>
                            <TableCell sx={{ fontWeight: 'bold', width: '55%' }}>Mensaje</TableCell>
                            <TableCell sx={{ fontWeight: 'bold', width: '10%', textAlign: 'center' }}>Estado</TableCell>
                            <TableCell sx={{ fontWeight: 'bold', width: '10%', textAlign: 'center' }}>Acciones</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {avisos.map((aviso, index) => (
                            <TableRow key={aviso.id} hover>
                                <TableCell>{index + 1}</TableCell>
                                <TableCell sx={{ fontWeight: 'bold' }}>{aviso.titulo}</TableCell>
                                <TableCell>{aviso.mensaje}</TableCell>
                                <TableCell align="center"><Chip label="ACTIVO" size="small" color="success" /></TableCell>
                                
                                {/* ---> SE AGREGA EL EVENTO onClick AL BOTÓN <--- */}
                                <TableCell align="center">
                                    <IconButton color="error" onClick={() => handleEliminar(aviso.id)}>
                                        <DeleteIcon />
                                    </IconButton>
                                </TableCell>
                                
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
        </Box>
    );
}