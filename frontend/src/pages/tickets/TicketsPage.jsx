import { useState, useEffect } from 'react';
import { Box, Button, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography, Chip, Snackbar, Alert } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import CheckIcon from '@mui/icons-material/Check';
import { useNavigate, useLocation } from 'react-router-dom';
import api from '../../services/api';

export default function TicketsPage() {
    const [historialTickets, setHistorialTickets] = useState([]);
    const navigate = useNavigate();
    const location = useLocation();

    // Estados para la alerta (Snackbar)
    const [openSnackbar, setOpenSnackbar] = useState(false);
    const [snackbarMessage, setSnackbarMessage] = useState('');

    const cargarHistorial = async () => {
        try {
            const response = await api.get('/v1/tickets/mis-tickets');
            setHistorialTickets(response.data);
        } catch (error) {
            console.error("Error al cargar el historial:", error);
        }
    };

    useEffect(() => { 
        cargarHistorial(); 
        
        // Verificamos si venimos del formulario con un mensaje de éxito
        if (location.state && location.state.mensajeExito) {
            setSnackbarMessage(location.state.mensajeExito);
            setOpenSnackbar(true);
            
            // Limpiamos el historial de navegación para que la alerta no vuelva a salir si recargan la página (F5)
            window.history.replaceState({}, document.title);
        }
    }, [location]);

    const handleCloseSnackbar = (event, reason) => {
        if (reason === 'clickaway') return;
        setOpenSnackbar(false);
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

    return (
        <Box sx={{ p: { xs: 1, sm: 3 }, maxWidth: 1200, mx: 'auto' }}>
            
            {/* ALERTA FLOTANTE ESTILO INSTITUCIONAL (CENTRO ARRIBA) */}
            <Snackbar 
                open={openSnackbar} 
                autoHideDuration={5000} // Se oculta sola después de 5 segundos
                onClose={handleCloseSnackbar}
                anchorOrigin={{ vertical: 'top', horizontal: 'center' }} // Centrado en la parte superior
                sx={{ top: { xs: 20, sm: 80 } }} // Margen superior para que no choque con tu Navbar
            >
                <Alert 
                    onClose={handleCloseSnackbar} 
                    icon={<CheckIcon sx={{ color: 'white', fontSize: 28 }} />} // Usamos el icono que ya funciona
                    sx={{ 
                        backgroundColor: '#5c0a28', // Color vino institucional
                        color: 'white',
                        minWidth: { xs: '300px', sm: '600px' }, // Mucho más ancho, como en tu captura
                        padding: '16px 24px',
                        fontSize: '1.1rem',
                        fontWeight: '600',
                        borderRadius: '8px',
                        boxShadow: '0px 10px 20px rgba(92, 10, 40, 0.4)', // Sombra para darle profundidad
                        alignItems: 'center',
                        '& .MuiAlert-action': {
                            color: 'white', // La tachita de cerrar en color blanco
                            padding: '0 8px'
                        }
                    }}
                >
                    {snackbarMessage}
                </Alert>
            </Snackbar>

            <Box sx={{ 
                display: 'flex', 
                flexDirection: { xs: 'column', sm: 'row' }, 
                justifyContent: 'space-between', 
                alignItems: { xs: 'flex-start', sm: 'center' }, 
                gap: 2, 
                mb: 4 
            }}>
                <Typography variant="h4" sx={{ color: '#2c3e50', fontWeight: 'bold', fontSize: { xs: '1.8rem', sm: '2.125rem' } }}>
                    Historial de Tickets
                </Typography>
                
                <Button 
                    variant="contained" 
                    color="primary"
                    startIcon={<AddIcon />} 
                    onClick={() => navigate('/empleado/nuevo')}
                >
                    Levantar Ticket
                </Button>
            </Box>

            <TableContainer component={Paper} elevation={2} sx={{ borderRadius: 3, overflow: 'hidden' }}>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell>ID</TableCell>
                            <TableCell>Título</TableCell>
                            <TableCell sx={{ display: { xs: 'none', md: 'table-cell' } }}>Descripción</TableCell>
                            <TableCell>Estatus</TableCell>
                            <TableCell align="center">Acción</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {historialTickets.length === 0 ? (
                            <TableRow>
                                <TableCell colSpan={5} align="center" sx={{ py: 4 }}>
                                    No hay tickets registrados en esta área.
                                </TableCell>
                            </TableRow>
                        ) : (
                            historialTickets.map((t) => (
                                <TableRow key={t.id} hover>
                                    <TableCell sx={{ color: '#64748b' }}>#{t.id}</TableCell>
                                    <TableCell sx={{ fontWeight: '500' }}>{t.titulo}</TableCell>
                                    <TableCell sx={{ display: { xs: 'none', md: 'table-cell' }, maxWidth: 250, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                                        {t.descripcion}
                                    </TableCell>
                                    <TableCell>
                                        <Chip 
                                            label={t.estatus} 
                                            color={t.estatus === 'CERRADO' ? 'success' : t.estatus === 'ABIERTO' ? 'warning' : 'default'} 
                                            size="small" 
                                            sx={{ fontWeight: 'bold' }}
                                        />
                                    </TableCell>
                                    <TableCell align="center">
                                        {t.estatus !== 'CERRADO' ? (
                                            <Button
                                                variant="outlined"
                                                color="primary" 
                                                size="small"
                                                startIcon={<CheckIcon />}
                                                onClick={() => handleFinalizarTicket(t.id)}
                                            >
                                                Finalizar
                                            </Button>
                                        ) : (
                                            <Typography variant="body2" sx={{ color: '#94a3b8', fontStyle: 'italic' }}>
                                                Resuelto
                                            </Typography>
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