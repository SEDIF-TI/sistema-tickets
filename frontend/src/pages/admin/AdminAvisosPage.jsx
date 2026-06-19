import { useState, useEffect } from 'react';
import { 
    Box, Typography, Paper, TextField, Button, 
    Table, TableBody, TableCell, TableContainer, 
    TableHead, TableRow, CircularProgress 
} from '@mui/material';
import CampaignIcon from '@mui/icons-material/Campaign';
import SendIcon from '@mui/icons-material/Send';
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
            cargarAvisos(); // Recargamos la tabla para ver el nuevo aviso
        } catch (error) {
            console.error("Error al crear aviso:", error);
            alert("Hubo un error al publicar el aviso.");
        } finally {
            setCargando(false);
        }
    };

    return (
        <Box sx={{ width: '100%' }}>
            <Typography variant="h4" fontWeight="bold" color="primary" sx={{ mb: 4, display: 'flex', alignItems: 'center', gap: 1 }}>
                <CampaignIcon fontSize="large" /> Gestión de Avisos Globales
            </Typography>

            {/* Formulario para nuevo aviso */}
            <Paper elevation={3} sx={{ p: 3, mb: 4, borderRadius: 2 }}>
                <Typography variant="h6" sx={{ mb: 2, fontWeight: 'bold' }}>Publicar Nuevo Aviso</Typography>
                <form onSubmit={handleCrearAviso}>
                    <TextField
                        fullWidth
                        label="Título del Aviso"
                        variant="outlined"
                        value={titulo}
                        onChange={(e) => setTitulo(e.target.value)}
                        required
                        sx={{ mb: 2 }}
                    />
                    <TextField
                        fullWidth
                        label="Mensaje"
                        variant="outlined"
                        multiline
                        rows={3}
                        value={mensaje}
                        onChange={(e) => setMensaje(e.target.value)}
                        required
                        sx={{ mb: 2 }}
                    />
                    <Button 
                        type="submit" 
                        variant="contained" 
                        color="primary" 
                        endIcon={cargando ? <CircularProgress size={20} color="inherit" /> : <SendIcon />}
                        disabled={cargando}
                    >
                        Publicar a todos los usuarios
                    </Button>
                </form>
            </Paper>

            {/* Historial de avisos activos */}
            <Typography variant="h6" sx={{ mb: 2, fontWeight: 'bold' }}>Avisos Activos Publicados</Typography>
            <TableContainer component={Paper} elevation={3} sx={{ borderRadius: 2 }}>
                <Table>
                    <TableHead sx={{ backgroundColor: '#f3f4f6' }}>
                        <TableRow>
                            <TableCell sx={{ fontWeight: 'bold' }}>ID</TableCell>
                            <TableCell sx={{ fontWeight: 'bold' }}>Título</TableCell>
                            <TableCell sx={{ fontWeight: 'bold' }}>Mensaje</TableCell>
                            <TableCell sx={{ fontWeight: 'bold' }}>Estado</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {avisos.length === 0 ? (
                            <TableRow>
                                <TableCell colSpan={4} align="center">No hay avisos activos.</TableCell>
                            </TableRow>
                        ) : (
                            avisos.map((aviso) => (
                                <TableRow key={aviso.id}>
                                    <TableCell>{aviso.id}</TableCell>
                                    <TableCell sx={{ fontWeight: 'bold' }}>{aviso.titulo}</TableCell>
                                    <TableCell>{aviso.mensaje}</TableCell>
                                    <TableCell>Activo</TableCell>
                                </TableRow>
                            ))
                        )}
                    </TableBody>
                </Table>
            </TableContainer>
        </Box>
    );
}