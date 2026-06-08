import { useEffect, useState } from 'react';
import { 
    Typography, Box, Chip, Paper, Table, TableBody, 
    TableCell, TableContainer, TableHead, TableRow, Button,
    Dialog, DialogTitle, DialogContent, DialogActions, TextField, IconButton
} from '@mui/material';
import InfoIcon from '@mui/icons-material/Info';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import VisibilityIcon from '@mui/icons-material/Visibility'; // Importamos el icono de ver
import SoporteLayout from '../../components/SoporteLayout.jsx';
import { useWebSocket } from '../../context/WebSocketContext.jsx';

export default function PanelSoporte() {
    const { stompClient, isConnected } = useWebSocket();
    
    const [tickets, setTickets] = useState([
        { id: 40, fechaInicio: '21/05/2026 21:07', fechaFin: '21/05/2026 23:45', solicitante: 'JOSE', departamento: 'DEPARTAMENTO DE SOPORTE TÉCNICO', estado: 'Pendiente' },
        { id: 42, fechaInicio: '22/05/2026 09:15', fechaFin: '--/--/---- --:--', solicitante: 'MARÍA', departamento: 'ADMINISTRACIÓN', estado: 'Pendiente' },
    ]);

    const [open, setOpen] = useState(false);
    const [justificacion, setJustificacion] = useState('');
    const [ticketSeleccionado, setTicketSeleccionado] = useState(null);

    const handleOpen = (ticket) => {
        setTicketSeleccionado(ticket);
        setOpen(true);
    };

    const handleClose = () => {
        setOpen(false);
        setJustificacion('');
    };

    const handleGuardar = () => {
        console.log(`Guardando justificación para el ticket ${ticketSeleccionado?.id}:`, justificacion);
        handleClose();
    };

    useEffect(() => {
        if (isConnected && stompClient) {
            const suscripcion = stompClient.subscribe('/topic/tickets-soporte', (mensaje) => {
                const nuevoTicket = JSON.parse(mensaje.body);
                setTickets((prev) => [nuevoTicket, ...prev]);
            });
            return () => suscripcion.unsubscribe();
        }
    }, [isConnected, stompClient]);

    return (
        <SoporteLayout>
            <Box sx={{ mb: 4, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="h4" fontWeight="bold" color="primary">
                    Tickets Asignados
                </Typography>
                <Chip 
                    label={isConnected ? "En línea" : "Desconectado"} 
                    color={isConnected ? "success" : "error"} 
                    variant="outlined"
                />
            </Box>

            <TableContainer component={Paper} elevation={3} sx={{ borderRadius: 2 }}>
                <Table sx={{ minWidth: 650 }} aria-label="tabla de tickets asignados">
                    <TableHead sx={{ backgroundColor: 'primary.main' }}>
                        <TableRow>
                            <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>ID</TableCell>
                            <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Fecha Inicio</TableCell>
                            <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Fecha Fin</TableCell>
                            <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Solicitante</TableCell>
                            <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Departamento</TableCell>
                            {/* NUEVA COLUMNA PARA VER TICKET */}
                            <TableCell sx={{ color: 'white', fontWeight: 'bold' }} align="center">Detalles</TableCell>
                            <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Estatus</TableCell>
                            <TableCell sx={{ color: 'white', fontWeight: 'bold' }} align="center">Acciones</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {tickets.length === 0 ? (
                            <TableRow>
                                <TableCell colSpan={8} align="center" sx={{ py: 3 }}>
                                    <Typography variant="body1" color="textSecondary">
                                        No tienes tickets asignados en este momento.
                                    </Typography>
                                </TableCell>
                            </TableRow>
                        ) : (
                            tickets.map((ticket) => (
                                <TableRow key={ticket.id} hover sx={{ '&:last-child td, &:last-child th': { border: 0 } }}>
                                    <TableCell component="th" scope="row" sx={{ fontWeight: 'bold' }}>
                                        {ticket.id}
                                    </TableCell>
                                    <TableCell>{ticket.fechaInicio}</TableCell>
                                    <TableCell>{ticket.fechaFin}</TableCell>
                                    <TableCell>{ticket.solicitante}</TableCell>
                                    <TableCell>{ticket.departamento}</TableCell>
                                    
                                    {/* NUEVO BOTÓN DE VER TICKET */}
                                    <TableCell align="center">
                                        <Button 
                                            variant="outlined" 
                                            size="small" 
                                            startIcon={<VisibilityIcon />} 
                                            sx={{ textTransform: 'none', borderRadius: 2 }}
                                        >
                                            Ver Ticket
                                        </Button>
                                    </TableCell>

                                    <TableCell>
                                        <Chip 
                                            label={ticket.estado} 
                                            size="small" 
                                            color="warning" 
                                            sx={{ fontWeight: 'bold', borderRadius: 1 }}
                                        />
                                    </TableCell>
                                    <TableCell align="center">
                                        <IconButton color="primary" onClick={() => handleOpen(ticket)}>
                                            <InfoIcon />
                                        </IconButton>
                                        <IconButton color="success">
                                            <CheckCircleIcon />
                                        </IconButton>
                                    </TableCell>
                                </TableRow>
                            ))
                        )}
                    </TableBody>
                </Table>
            </TableContainer>

            {/* Modal de Justificación Simplificado */}
            <Dialog open={open} onClose={handleClose} fullWidth maxWidth="sm">
                <DialogTitle sx={{ backgroundColor: 'primary.main', color: 'white' }}>
                    ticket
                </DialogTitle>
                <DialogContent sx={{ mt: 2 }}>
                    <Typography variant="subtitle2" sx={{ mb: 1, fontWeight: 'bold' }}>
                        Justificación:
                    </Typography>
                    <TextField
                        autoFocus
                        margin="dense"
                        fullWidth
                        multiline
                        rows={4}
                        value={justificacion}
                        onChange={(e) => setJustificacion(e.target.value)}
                        variant="outlined"
                    />
                </DialogContent>
                <DialogActions sx={{ p: 2 }}>
                    <Button 
                        onClick={handleGuardar} 
                        variant="contained" 
                        color="primary"
                        sx={{ textTransform: 'none' }}
                    >
                        Confirmar
                    </Button>
                    <Button 
                        onClick={handleClose} 
                        variant="contained" 
                        sx={{ backgroundColor: '#4b5563', color: 'white', '&:hover': { backgroundColor: '#374151' }, textTransform: 'none' }}
                    >
                        Cerrar
                    </Button>
                </DialogActions>
            </Dialog>
        </SoporteLayout>
    );
}