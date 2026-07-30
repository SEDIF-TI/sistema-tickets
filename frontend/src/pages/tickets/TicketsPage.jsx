import { useState, useEffect, useContext } from 'react';
import { Box, Button, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography, Chip, Snackbar, Alert } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import CheckIcon from '@mui/icons-material/Check';
import { useNavigate, useLocation } from 'react-router-dom';
import api from '../../services/api';
import { AuthContext } from '../../context/AuthContext.jsx'; 

// --- NUEVO: Importamos tu hook de red ---
import { useNetworkStatus } from '../../hooks/useNetworkStatus.jsx';
// ----------------------------------------

const COLOR_GUINDA = '#801A36';

export default function TicketsPage() {
    const { user } = useContext(AuthContext); 
    const navigate = useNavigate();
    const location = useLocation();

    // --- NUEVO: Instanciamos el estado de la red ---
    const isOffline = useNetworkStatus();
    // -----------------------------------------------

    const [historialTickets, setHistorialTickets] = useState([]);
    const [openSnackbar, setOpenSnackbar] = useState(false);
    const [snackbarMessage, setSnackbarMessage] = useState('');

    const userRole = user?.rol || user?.role || user?.rolNombre || '';
    const cleanRole = userRole.replace('ROLE_', '').toUpperCase();

    const formatearFecha = (fecha) => {
        if (!fecha) return '--/--/----';
        const d = new Date(fecha);
        return `${d.getDate().toString().padStart(2, '0')}/${(d.getMonth() + 1).toString().padStart(2, '0')}/${d.getFullYear().toString().slice(-2)} ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`;
    };

    const cargarHistorial = async () => {
        // Si no hay red, mejor ni intentamos hacer la petición para evitar errores en consola
        if (isOffline) return; 
        try {
            const response = await api.get('/v1/tickets/mis-tickets');
            setHistorialTickets(response.data);
        } catch (error) {
            console.error("Error al cargar el historial:", error);
        }
    };

    const handleFinalizarTicket = async (ticketId) => {
        // --- NUEVO: Bloqueo lógico por si logran burlar el botón ---
        if (isOffline) {
            alert("No hay conexión a internet. No puedes finalizar tickets en este momento.");
            return;
        }

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
    }, [location, isOffline]); // Añadimos isOffline para que recargue el historial automáticamente al volver el internet

    return (
        <Box sx={{ px: { xs: 2, md: 4 }, py: 3, width: '100%', boxSizing: 'border-box' }}>
            
            <Snackbar open={openSnackbar} autoHideDuration={5000} onClose={() => setOpenSnackbar(false)} anchorOrigin={{ vertical: 'top', horizontal: 'center' }}>
                <Alert onClose={() => setOpenSnackbar(false)} severity="info">
                    {snackbarMessage}
                </Alert>
            </Snackbar>

            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
                <Typography variant="h4" sx={{ fontWeight: 'bold', color: COLOR_GUINDA }}>
                    {cleanRole === 'ADMINISTRADOR' ? 'Bitácora Global de Tickets' : 'Historial de Tickets'}
                </Typography>
                
                {cleanRole !== 'ADMINISTRADOR' && (
                    <Button 
                        variant="contained" 
                        startIcon={<AddIcon />} 
                        onClick={() => navigate('/tickets/nuevo')} 
                        disabled={isOffline} // <-- BLOQUEO DEL BOTÓN NUEVO TICKET
                        sx={{ 
                            bgcolor: isOffline ? 'grey.400' : COLOR_GUINDA, 
                            '&:hover': { bgcolor: isOffline ? 'grey.400' : '#5c0a28' }
                        }}
                    >
                        {isOffline ? 'Sin Conexión' : 'Levantar Ticket'}
                    </Button>
                )}
            </Box>

            <TableContainer component={Paper} elevation={0} sx={{ border: '1px solid #e2e8f0', borderRadius: 2, overflow: 'hidden' }}>
                <Table sx={{ minWidth: 1000 }}>
                    <TableHead>
                        <TableRow sx={{ bgcolor: COLOR_GUINDA }}>
                            <TableCell sx={{ color: 'white', fontWeight: 'bold', width: '5%' }}>ID</TableCell>
                            <TableCell sx={{ color: 'white', fontWeight: 'bold', width: '15%' }}>Solicitante</TableCell>
                            <TableCell sx={{ color: 'white', fontWeight: 'bold', width: '15%' }}>Departamento</TableCell>
                            <TableCell sx={{ color: 'white', fontWeight: 'bold', width: '25%' }}>Título</TableCell>
                            <TableCell sx={{ color: 'white', fontWeight: 'bold', width: '10%' }}>Inicio</TableCell>
                            <TableCell sx={{ color: 'white', fontWeight: 'bold', width: '10%' }}>Fin</TableCell>
                            <TableCell sx={{ color: 'white', fontWeight: 'bold', width: '10%', textAlign: 'center' }}>Estatus</TableCell>
                            <TableCell sx={{ color: 'white', fontWeight: 'bold', width: '10%', textAlign: 'center' }}>Acción</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {historialTickets.length === 0 ? (
                            <TableRow>
                                <TableCell colSpan={8} align="center" sx={{ py: 6 }}>
                                    <Typography color="textSecondary">No hay tickets registrados.</Typography>
                                </TableCell>
                            </TableRow>
                        ) : (
                            historialTickets.map((t) => (
                                <TableRow key={t.id} hover sx={{ transition: '0.2s', '&:hover': { bgcolor: '#f9f9f9' } }}>
                                    <TableCell sx={{ fontWeight: 'bold' }}>#{t.id}</TableCell>
                                    <TableCell sx={{ fontWeight: '500' }}>{t.solicitante || 'Usuario'}</TableCell>
                                    <TableCell>{t.departamento || 'Área'}</TableCell>
                                    <TableCell>{t.titulo}</TableCell>
                                    <TableCell sx={{ whiteSpace: 'nowrap', fontSize: '0.85rem' }}>{formatearFecha(t.fechaCreacion)}</TableCell>
                                    <TableCell sx={{ whiteSpace: 'nowrap', fontSize: '0.85rem' }}>{formatearFecha(t.fechaFin)}</TableCell>
                                    <TableCell align="center">
                                        <Chip label={t.estatus} color={t.estatus === 'CERRADO' ? 'default' : 'warning'} size="small" sx={{ fontWeight: 'bold', minWidth: '80px' }} />
                                    </TableCell>
                                    <TableCell align="center">
                                        {t.estatus !== 'CERRADO' ? (
                                            cleanRole !== 'ADMINISTRADOR' ? (
                                                <Button 
                                                    size="small" 
                                                    variant="outlined" 
                                                    color="inherit" 
                                                    startIcon={<CheckIcon />} 
                                                    onClick={() => handleFinalizarTicket(t.id)}
                                                    disabled={isOffline} // <-- BLOQUEO DEL BOTÓN DE FINALIZAR
                                                >
                                                    Finalizar
                                                </Button>
                                            ) : (
                                                <Typography variant="body2" color="textSecondary" sx={{ fontStyle: 'italic' }}>En proceso</Typography>
                                            )
                                        ) : (
                                            <Typography variant="body2" color="textSecondary" sx={{ fontWeight: 'bold' }}>Finalizado</Typography>
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