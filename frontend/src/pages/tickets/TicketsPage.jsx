import { useState, useEffect } from 'react';
import { Box, Button, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography, Chip, Snackbar, Alert } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import CheckIcon from '@mui/icons-material/Check';
import { useNavigate, useLocation } from 'react-router-dom';
import api from '../../services/api';

const COLOR_GUINDA = '#801A36';

export default function TicketsPage() {
    const [historialTickets, setHistorialTickets] = useState([]);
    const navigate = useNavigate();
    const location = useLocation();

    const [openSnackbar, setOpenSnackbar] = useState(false);
    const [snackbarMessage, setSnackbarMessage] = useState('');

    // Función de formato compacto
    const formatearFecha = (fecha) => {
        if (!fecha) return '--/--/----';
        const d = new Date(fecha);
        return `${d.getDate().toString().padStart(2, '0')}/${(d.getMonth() + 1).toString().padStart(2, '0')}/${d.getFullYear().toString().slice(-2)} ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`;
    };

    const cargarHistorial = async () => {
        try {
            const response = await api.get('/v1/tickets/mis-tickets');
            setHistorialTickets(response.data);
        } catch (error) {
            console.error("Error al cargar el historial:", error);
        }
    };

    const handleFinalizarTicket = async (ticketId) => {
        const confirmar = window.confirm('¿Estás seguro de que tu problema fue resuelto y deseas cerrar este ticket?');
        if (confirmar) {
            try {
                await api.put(`/v1/tickets/${ticketId}/finalizar`);
                cargarHistorial(); 
            } catch (error) {
                console.error("Error al finalizar el ticket:", error);
                alert("Hubo un error al intentar cerrar el ticket.");
            }
        }
    };

    useEffect(() => { 
        cargarHistorial(); 
        if (location.state?.mensajeExito) {
            setSnackbarMessage(location.state.mensajeExito);
            setOpenSnackbar(true);
            window.history.replaceState({}, document.title);
        }
    }, [location]);

    return (
        <Box sx={{ p: { xs: 1, sm: 3 }, maxWidth: 1300, mx: 'auto' }}>
            
            <Snackbar open={openSnackbar} autoHideDuration={5000} onClose={() => setOpenSnackbar(false)} anchorOrigin={{ vertical: 'top', horizontal: 'center' }}>
                <Alert onClose={() => setOpenSnackbar(false)} sx={{ backgroundColor: COLOR_GUINDA, color: 'white' }}>
                    {snackbarMessage}
                </Alert>
            </Snackbar>

            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
                <Typography variant="h4" sx={{ fontWeight: 'bold', color: COLOR_GUINDA }}>Historial de Tickets</Typography>
                <Button variant="contained" startIcon={<AddIcon />} onClick={() => navigate('/tickets/nuevo')} sx={{ bgcolor: COLOR_GUINDA }}>
                    Levantar Ticket
                </Button>
            </Box>

            <TableContainer component={Paper} elevation={2} sx={{ borderRadius: 3, overflow: 'hidden' }}>
                <Table>
                    <TableHead>
                        <TableRow sx={{ bgcolor: COLOR_GUINDA }}>
                            <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>ID</TableCell>
                            <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Solicitante</TableCell>
                            <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Departamento</TableCell>
                            <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Título</TableCell>
                            <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Inicio</TableCell>
                            <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Fin</TableCell>
                            <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Estatus</TableCell>
                            <TableCell sx={{ color: 'white', fontWeight: 'bold', textAlign: 'center' }}>Acción</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {historialTickets.length === 0 ? (
                            <TableRow>
                                <TableCell colSpan={8} align="center" sx={{ py: 4 }}>No hay tickets registrados.</TableCell>
                            </TableRow>
                        ) : (
                            historialTickets.map((t) => (
                                <TableRow key={t.id} hover>
                                    <TableCell sx={{ fontWeight: 'bold' }}>#{t.id}</TableCell>
                                    <TableCell>{t.solicitante}</TableCell>
                                    <TableCell>{t.departamento}</TableCell>
                                    <TableCell>{t.titulo}</TableCell>
                                    <TableCell sx={{ whiteSpace: 'nowrap', fontSize: '0.85rem' }}>{formatearFecha(t.fechaCreacion)}</TableCell>
                                    <TableCell sx={{ whiteSpace: 'nowrap', fontSize: '0.85rem' }}>{formatearFecha(t.fechaFin)}</TableCell>
                                    <TableCell>
                                        <Chip 
                                            label={t.estatus} 
                                            color={t.estatus === 'CERRADO' ? 'success' : 'warning'} 
                                            size="small" 
                                            sx={{ fontWeight: 'bold' }} 
                                        />
                                    </TableCell>
                                    <TableCell align="center">
                                        {t.estatus !== 'CERRADO' && (
                                            <Button size="small" variant="outlined" color="inherit" startIcon={<CheckIcon />} onClick={() => handleFinalizarTicket(t.id)}>Finalizar</Button>
                                        )}
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