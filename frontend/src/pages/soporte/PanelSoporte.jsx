import { useEffect, useState } from 'react';
import { 
    Typography, Box, Chip, Paper, Table, TableBody, 
    TableCell, TableContainer, TableHead, TableRow, Button,
    Dialog, DialogTitle, DialogContent, DialogActions, TextField, IconButton,
    TablePagination, InputAdornment, CircularProgress
} from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import VisibilityIcon from '@mui/icons-material/Visibility';
import SearchIcon from '@mui/icons-material/Search';
import { useWebSocket } from '../../context/WebSocketContext.jsx';
import api from '../../services/api';

export default function PanelSoporte() {
    const { stompClient, isConnected } = useWebSocket();
    
    // Estados de datos
    const [tickets, setTickets] = useState([]);
    const [cargando, setCargando] = useState(true);

    // Estados para el Modal
    const [open, setOpen] = useState(false);
    const [justificacion, setJustificacion] = useState('');
    const [ticketSeleccionado, setTicketSeleccionado] = useState(null);

    // Estados para la Paginación y Búsqueda
    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(10);
    const [busqueda, setBusqueda] = useState('');

    useEffect(() => {
        const cargarTicketsHistoricos = async () => {
            try {
                const respuesta = await api.get('/v1/tickets'); 
                setTickets(respuesta.data);
            } catch (error) {
                console.error("Error al cargar el histórico de tickets:", error);
            } finally {
                setCargando(false);
            }
        };

        cargarTicketsHistoricos();
    }, []);

    useEffect(() => {
        if (isConnected && stompClient && stompClient.connected) {
            try {
                const suscripcion = stompClient.subscribe('/topic/tickets-soporte', (mensaje) => {
                    const nuevoTicket = JSON.parse(mensaje.body);
                    setTickets((prev) => [nuevoTicket, ...prev]);
                });
                return () => {
                    if (suscripcion) suscripcion.unsubscribe();
                };
            } catch (error) {
                console.error("Error en la suscripción WebSocket:", error);
            }
        }
    }, [isConnected, stompClient]);

    const handleOpen = (ticket) => {
        setTicketSeleccionado(ticket);
        setOpen(true);
    };

    const handleClose = () => {
        setOpen(false);
        setJustificacion('');
    };

    const handleGuardar = () => {
        // Aquí conectaremos luego el endpoint para guardar la justificación en la BD
        console.log(`Guardando justificación para el ticket ${ticketSeleccionado?.id}:`, justificacion);
        handleClose();
    };

    const handleChangePage = (event, newPage) => {
        setPage(newPage);
    };

    const handleChangeRowsPerPage = (event) => {
        setRowsPerPage(parseInt(event.target.value, 10));
        setPage(0);
    };

    const ticketsFiltrados = tickets.filter((ticket) => {
        const search = busqueda.toLowerCase();
        const solicitante = (ticket.solicitante || '').toLowerCase();
        const departamento = (ticket.departamento || '').toLowerCase();
        const idStr = (ticket.id || '').toString();

        return solicitante.includes(search) || departamento.includes(search) || idStr.includes(search);
    });

    const ticketsPaginados = ticketsFiltrados.slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage);

    return (
        <Box sx={{ width: '100%' }}>
            <Box sx={{ mb: 4, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="h4" fontWeight="bold" color="primary">
                    Tickets Asignados
                </Typography>
                <Chip 
                    label={isConnected ? "WebSocket Conectado" : "WebSocket Desconectado"} 
                    color={isConnected ? "success" : "error"} 
                    variant="outlined"
                />
            </Box>

            <Paper elevation={3} sx={{ borderRadius: 2, p: 2, mb: 4 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                    <Typography variant="body2" color="textSecondary">
                        Mostrando registros de soporte
                    </Typography>
                    <TextField
                        size="small"
                        placeholder="Buscar ticket..."
                        value={busqueda}
                        onChange={(e) => {
                            setBusqueda(e.target.value);
                            setPage(0);
                        }}
                        InputProps={{
                            startAdornment: (
                                <InputAdornment position="start">
                                    <SearchIcon />
                                </InputAdornment>
                            ),
                        }}
                        sx={{ minWidth: 250 }}
                    />
                </Box>

                <TableContainer>
                    <Table sx={{ minWidth: 650 }} aria-label="tabla de tickets asignados">
                        <TableHead sx={{ backgroundColor: 'primary.main' }}>
                            <TableRow>
                                <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>ID</TableCell>
                                <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Fecha Inicio</TableCell>
                                <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Fecha Fin</TableCell>
                                <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Solicitante</TableCell>
                                <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Departamento</TableCell>
                                <TableCell sx={{ color: 'white', fontWeight: 'bold' }} align="center">Detalles</TableCell>
                                <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Estatus</TableCell>
                                <TableCell sx={{ color: 'white', fontWeight: 'bold' }} align="center">Acciones</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {cargando ? (
                                <TableRow>
                                    <TableCell colSpan={8} align="center" sx={{ py: 4 }}>
                                        <CircularProgress color="primary" />
                                        <Typography sx={{ mt: 2 }}>Cargando tickets...</Typography>
                                    </TableCell>
                                </TableRow>
                            ) : ticketsFiltrados.length === 0 ? (
                                <TableRow>
                                    <TableCell colSpan={8} align="center" sx={{ py: 3 }}>
                                        <Typography variant="body1" color="textSecondary">
                                            No se encontraron tickets con esos criterios.
                                        </Typography>
                                    </TableCell>
                                </TableRow>
                            ) : (
                                ticketsPaginados.map((ticket) => (
                                    <TableRow key={ticket.id} hover sx={{ '&:last-child td, &:last-child th': { border: 0 } }}>
                                        <TableCell component="th" scope="row" sx={{ fontWeight: 'bold' }}>
                                            {ticket.id}
                                        </TableCell>
                                        
                                        {/* FECHA: Busca fechaCreacion desde el backend */}
                                        <TableCell>{ticket.fechaCreacion ? new Date(ticket.fechaCreacion).toLocaleString() : 'N/A'}</TableCell>
                                        
                                        <TableCell>{ticket.fechaFin || '--/--/----'}</TableCell>
                                        <TableCell>{ticket.solicitante || 'Usuario'}</TableCell>
                                        <TableCell>{ticket.departamento || 'Área'}</TableCell>
                                        
                                        <TableCell align="center">
                                            <Button 
                                                variant="outlined" 
                                                size="small" 
                                                onClick={() => handleOpen(ticket)}
                                                startIcon={<VisibilityIcon />} 
                                                sx={{ textTransform: 'none', borderRadius: 2 }}
                                            >
                                                Ver Ticket
                                            </Button>
                                        </TableCell>

                                        <TableCell>
                                            <Chip 
                                                label={ticket.estado || ticket.estatus || 'Abierto'} 
                                                size="small" 
                                                color="warning" 
                                                sx={{ fontWeight: 'bold', borderRadius: 1 }}
                                            />
                                        </TableCell>
                                        <TableCell align="center">
                                            {/* El ícono de 'Info' (amarillo en tu dibujo) fue eliminado */}
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

                <TablePagination
                    rowsPerPageOptions={[5, 10, 25]}
                    component="div"
                    count={ticketsFiltrados.length}
                    rowsPerPage={rowsPerPage}
                    page={page}
                    onPageChange={handleChangePage}
                    onRowsPerPageChange={handleChangeRowsPerPage}
                    labelRowsPerPage="Mostrar registros:"
                    labelDisplayedRows={({ from, to, count }) => `Mostrando ${from} a ${to} de ${count} entradas`}
                />
            </Paper>

            {/* MODAL / DIALOG ACTUALIZADO */}
            <Dialog open={open} onClose={handleClose} fullWidth maxWidth="sm">
                <DialogTitle sx={{ backgroundColor: 'primary.main', color: 'white' }}>
                    Detalle del Ticket #{ticketSeleccionado?.id}
                </DialogTitle>
                <DialogContent sx={{ mt: 2 }}>
                    
                    {/* Título y Descripción */}
                    <Typography variant="subtitle2" sx={{ fontWeight: 'bold' }}>Título:</Typography>
                    <Typography variant="body1" sx={{ mb: 2 }}>{ticketSeleccionado?.titulo}</Typography>

                    <Typography variant="subtitle2" sx={{ fontWeight: 'bold' }}>Descripción:</Typography>
                    <Paper variant="outlined" sx={{ p: 2, bgcolor: '#f9f9f9', mb: 3 }}>
                        <Typography variant="body2">{ticketSeleccionado?.descripcion}</Typography>
                    </Paper>

                    {/* Fila con SEDE (Negro) y ÁREA (Rojo) */}
                    <Box sx={{ display: 'flex', gap: 6, mb: 3 }}>
                        <Box>
                            <Typography variant="subtitle2" sx={{ fontWeight: 'bold' }}>Sede:</Typography>
                            {/* Si no hay sede, pone SEDIF por defecto */}
                            <Typography variant="body2">{ticketSeleccionado?.sede || 'SEDIF'}</Typography>
                        </Box>
                        <Box>
                            <Typography variant="subtitle2" sx={{ fontWeight: 'bold' }}>Área de origen:</Typography>
                            <Typography variant="body2">{ticketSeleccionado?.departamento || 'No especificada'}</Typography>
                        </Box>
                    </Box>

                    {/* CAJA DE TEXTO PARA JUSTIFICACIÓN (Amarillo) */}
                    <Typography variant="subtitle2" sx={{ fontWeight: 'bold' }}>Justificación / Notas:</Typography>
                    <TextField
                        autoFocus
                        margin="dense"
                        fullWidth
                        multiline
                        rows={3}
                        value={justificacion}
                        onChange={(e) => setJustificacion(e.target.value)}
                        variant="outlined"
                        placeholder="Escribe la justificación aquí..."
                    />

                </DialogContent>
                <DialogActions sx={{ p: 2 }}>
                    <Button onClick={handleClose} variant="contained" sx={{ backgroundColor: '#4b5563', color: 'white', '&:hover': { backgroundColor: '#374151' }, textTransform: 'none' }}>
                        Cerrar
                    </Button>
                    {/* BOTÓN DE CONFIRMAR (Verde) */}
                    <Button onClick={handleGuardar} variant="contained" color="primary" sx={{ textTransform: 'none' }}>
                        Confirmar
                    </Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
}