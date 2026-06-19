import { useState, useEffect } from 'react';
import { 
    Box, Typography, Paper, TextField, Button, 
    Table, TableBody, TableCell, TableContainer, 
    TableHead, TableRow, CircularProgress, Chip, IconButton, Tooltip
} from '@mui/material';
import CampaignIcon from '@mui/icons-material/Campaign';
import SendIcon from '@mui/icons-material/Send';
import CancelIcon from '@mui/icons-material/Cancel';
import api from '../../services/api';

export default function AdminAvisosPage() {
    const [titulo, setTitulo] = useState('');
    const [mensaje, setMensaje] = useState('');
    const [avisos, setAvisos] = useState([]);
    const [cargando, setCargando] = useState(false);

    const cargarAvisos = async () => {
        try {
            const respuesta = await api.get('/v1/avisos/activos');
            setAvisos(respuesta.data);
        } catch (error) {
            console.error("Error al cargar avisos:", error);
        }
    };

    useEffect(() => {
        cargarAvisos();
    }, []);

    const handleCrearAviso = async (e) => {
        e.preventDefault();
        if (!titulo.trim() || !mensaje.trim()) return;

        setCargando(true);
        try {
            await api.post('/v1/avisos', { titulo, mensaje });
            setTitulo('');
            setMensaje('');
            cargarAvisos();
        } catch (error) {
            console.error("Error al crear aviso:", error);
            alert("Hubo un error al publicar el aviso.");
        } finally {
            setCargando(false);
        }
    };

    // --- NUEVA FUNCIÓN PARA FINALIZAR EL AVISO ---
    const handleFinalizarAviso = async (id) => {
        if(window.confirm("¿Estás seguro de finalizar este aviso? Ya no aparecerá en las campanas de los usuarios.")) {
            try {
                await api.put(`/v1/avisos/${id}/desactivar`);
                cargarAvisos(); // Recargamos para que desaparezca de la lista de activos
            } catch (error) {
                console.error("Error al finalizar aviso:", error);
                alert("Hubo un error al finalizar el aviso.");
            }
        }
    };

    return (
        <Box sx={{ width: '100%' }}>
            <Typography variant="h4" fontWeight="bold" color="primary" sx={{ mb: 4, display: 'flex', alignItems: 'center', gap: 1 }}>
                <CampaignIcon fontSize="large" /> Gestión de Avisos Globales
            </Typography>

            <Paper elevation={3} sx={{ p: 3, mb: 4, borderRadius: 2 }}>
                <Typography variant="h6" sx={{ mb: 2, fontWeight: 'bold' }}>Publicar Nuevo Aviso</Typography>
                <form onSubmit={handleCrearAviso}>
                    <TextField fullWidth label="Título del Aviso" variant="outlined" value={titulo} onChange={(e) => setTitulo(e.target.value)} required sx={{ mb: 2 }} />
                    <TextField fullWidth label="Mensaje" variant="outlined" multiline rows={3} value={mensaje} onChange={(e) => setMensaje(e.target.value)} required sx={{ mb: 2 }} />
                    <Button type="submit" variant="contained" color="primary" endIcon={cargando ? <CircularProgress size={20} color="inherit" /> : <SendIcon />} disabled={cargando}>
                        Publicar a todos los usuarios
                    </Button>
                </form>
            </Paper>

            <Typography variant="h6" sx={{ mb: 2, fontWeight: 'bold' }}>Avisos Activos Publicados</Typography>
            <TableContainer component={Paper} elevation={3} sx={{ borderRadius: 2 }}>
                <Table>
                    {/* --- CABECERA CON COLOR INSTITUCIONAL --- */}
                    <TableHead sx={{ backgroundColor: '#5c0a28' }}>
                        <TableRow>
                            <TableCell sx={{ fontWeight: 'bold', color: 'white' }}>ID</TableCell>
                            <TableCell sx={{ fontWeight: 'bold', color: 'white' }}>Título</TableCell>
                            <TableCell sx={{ fontWeight: 'bold', color: 'white' }}>Mensaje</TableCell>
                            <TableCell align="center" sx={{ fontWeight: 'bold', color: 'white' }}>Estado</TableCell>
                            <TableCell align="center" sx={{ fontWeight: 'bold', color: 'white' }}>Acciones</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {avisos.length === 0 ? (
                            <TableRow>
                                <TableCell colSpan={5} align="center">No hay avisos activos.</TableCell>
                            </TableRow>
                        ) : (
                            avisos.map((aviso) => (
                                <TableRow key={aviso.id}>
                                    <TableCell>{aviso.id}</TableCell>
                                    <TableCell sx={{ fontWeight: 'bold' }}>{aviso.titulo}</TableCell>
                                    <TableCell>{aviso.mensaje}</TableCell>
                                    <TableCell align="center">
                                        <Chip label="ACTIVO" color="success" size="small" sx={{ fontWeight: 'bold' }} />
                                    </TableCell>
                                    <TableCell align="center">
                                        <Tooltip title="Finalizar Aviso">
                                            <IconButton color="error" onClick={() => handleFinalizarAviso(aviso.id)}>
                                                <CancelIcon />
                                            </IconButton>
                                        </Tooltip>
                                    </TableCell>
                                </TableRow>
                            ))
                        )}
                    </TableBody>
                </Table>
            </TableContainer>
        </Box>
    );
}