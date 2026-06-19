import { useEffect, useState, useContext } from 'react';
import { 
    Typography, Box, Chip, Paper, Table, TableBody, 
    TableCell, TableContainer, TableHead, TableRow, Button,
    Dialog, DialogTitle, DialogContent, DialogActions, TextField, IconButton,
    TablePagination, InputAdornment, CircularProgress
} from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import VisibilityIcon from '@mui/icons-material/Visibility';
import SearchIcon from '@mui/icons-material/Search';
import DirectionsRunIcon from '@mui/icons-material/DirectionsRun';
import { useWebSocket } from '../../context/WebSocketContext.jsx';
import { AuthContext } from '../../context/AuthContext.jsx'; // <-- Nuevo import
import api from '../../services/api';

export default function PanelSoporte() {
    const { stompClient, isConnected } = useWebSocket();
    const { user } = useContext(AuthContext); // <-- Extraemos al usuario logueado
    
    const [tickets, setTickets] = useState([]);
    const [cargando, setCargando] = useState(true);

    const [openDetalle, setOpenDetalle] = useState(false);
    const [openResolucion, setOpenResolucion] = useState(false);
    
    const [justificacion, setJustificacion] = useState('');
    const [ticketSeleccionado, setTicketSeleccionado] = useState(null);

    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(10);
    const [busqueda, setBusqueda] = useState('');

    // 1er useEffect: Carga el histórico desde la BD
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

    // 2do useEffect: Escucha los WebSockets en tiempo real con FILTRO
    useEffect(() => {
        // Aseguramos que 'user' exista antes de escuchar
        if (isConnected && stompClient && stompClient.connected && user) {
            try {
                const suscripcion = stompClient.subscribe('/topic/tickets-soporte', (mensaje) => {
                    const nuevoTicket = JSON.parse(mensaje.body);
                    
                    // FILTRO MÁGICO: Solo lo agregamos si el ID coincide con el del técnico
                    if (nuevoTicket.usuarioSoporteId === user.usuarioId) {
                        setTickets((prev) => [nuevoTicket, ...prev]);
                    }
                });
                return () => {
                    if (suscripcion) suscripcion.unsubscribe();
                };
            } catch (error) {
                console.error("Error en la suscripción WebSocket:", error);
            }
        }
    }, [isConnected, stompClient, user]); // <-- user agregado a las dependencias

    const handleAtenderTicket = async (ticket) => {
        try {
            await api.put(`/v1/tickets/${ticket.id}/atender`);
            setTickets((prev) => prev.map(t => t.id === ticket.id ? { ...t, estatus: 'EN PROCESO' } : t));
        } catch (error) {
            console.error("Error al marcar el ticket en camino:", error);
        }
    };

    const handleResolverTicket = async () => {
        if (!justificacion.trim()) {
            alert("Debes escribir una justificación antes de confirmar.");
            return;
        }

        try {
            await api.put(`/v1/tickets/${ticketSeleccionado.id}/resolver`, { justificacion });
            setTickets((prev) => prev.map(t => 
                t.id === ticketSeleccionado.id 
                ? { ...t, estatus: 'CERRADO', fechaFin: new Date().toISOString(), justificacion: justificacion } 
                : t
            ));
            handleCloseResolucion();
        } catch (error) {
            console.error("Error al resolver el ticket:", error);
        }
    };

    const handleOpenDetalle = (ticket) => {
        setTicketSeleccionado(ticket);
        setOpenDetalle(true);
    };

    const handleCloseDetalle = () => {
        setOpenDetalle(false);
        setTicketSeleccionado(null);
    };

    const handleOpenResolucion = (ticket) => {
        setTicketSeleccionado(ticket);
        setJustificacion('');
        setOpenResolucion(true);
    };

    const handleCloseResolucion = (event, reason) => {
        if (reason && reason === 'backdropClick') return;
        setOpenResolucion(false);
        setTicketSeleccionado(null);
        setJustificacion('');
    };

    const handleChangePage = (event, newPage) => setPage(newPage);
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
                                ticketsPaginados.map((ticket, index) => {
                                    // 🪄 Magia: Calculamos el número de fila consecutivo
                                    const numeroFila = page * rowsPerPage + index + 1;

                                    return (
                                        <TableRow key={ticket.id} hover sx={{ '&:last-child td, &:last-child th': { border: 0 } }}>
                                            <TableCell component="th" scope="row" sx={{ fontWeight: 'bold' }}>
                                                {/* Reemplazamos ticket.id por nuestra variable consecutiva */}
                                                {numeroFila}
                                            </TableCell>
                                            
                                            <TableCell>{ticket.fechaCreacion ? new Date(ticket.fechaCreacion).toLocaleString() : 'N/A'}</TableCell>
                                            
                                            <TableCell>{ticket.fechaFin ? new Date(ticket.fechaFin).toLocaleString() : '--/--/----'}</TableCell>
                                            
                                            <TableCell>{ticket.solicitante || 'Usuario'}</TableCell>
                                            <TableCell>{ticket.departamento || 'Área'}</TableCell>
                                            
                                            <TableCell align="center">
                                                <Button 
                                                    variant="outlined" 
                                                    size="small" 
                                                    onClick={() => handleOpenDetalle(ticket)}
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
                                                    sx={{ 
                                                        fontWeight: 'bold', 
                                                        borderRadius: 1,
                                                        backgroundColor: 
                                                            (ticket.estatus === 'RESUELTO' || ticket.estatus === 'CERRADO') ? 'primary.dark' : 
                                                            (ticket.estatus === 'EN PROCESO') ? 'primary.light' : 
                                                            '#e0e0e0', // Gris neutral para abierto
                                                        color: (ticket.estatus === 'ABIERTO' || ticket.estatus === 'ASIGNADO') ? 'text.primary' : 'white'
                                                    }}
                                                />
                                            </TableCell>
                                            
                                            <TableCell align="center">
                                                {(ticket.estatus === 'ASIGNADO' || ticket.estatus === 'ABIERTO') && (
                                                    <IconButton 
                                                        onClick={() => handleAtenderTicket(ticket)} 
                                                        title="Voy en camino"
                                                        sx={{ color: 'primary.light' }}
                                                    >
                                                        <DirectionsRunIcon />
                                                    </IconButton>
                                                )}
                                                
                                                {ticket.estatus === 'EN PROCESO' && (
                                                    <IconButton 
                                                        onClick={() => handleOpenResolucion(ticket)} 
                                                        title="Resolver Ticket"
                                                        sx={{ color: 'primary.main' }}
                                                    >
                                                        <CheckCircleIcon />
                                                    </IconButton>
                                                )}
                                                
                                                {(ticket.estatus === 'RESUELTO' || ticket.estatus === 'CERRADO') && (
                                                    <Typography variant="caption" color="textSecondary" sx={{ fontWeight: 'bold' }}>
                                                        Finalizado
                                                    </Typography>
                                                )}
                                            </TableCell>
                                        </TableRow>
                                    );
                                })
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

            {/* MODAL DE DETALLES */}
            <Dialog open={openDetalle} onClose={handleCloseDetalle} fullWidth maxWidth="sm">
                <DialogTitle sx={{ backgroundColor: 'primary.main', color: 'white' }}>
                    Detalle del Ticket #{ticketSeleccionado?.id}
                </DialogTitle>
                <DialogContent sx={{ mt: 2 }}>
                    <Typography variant="subtitle2" sx={{ fontWeight: 'bold' }}>Título:</Typography>
                    <Typography variant="body1" sx={{ mb: 2 }}>{ticketSeleccionado?.titulo}</Typography>

                    <Typography variant="subtitle2" sx={{ fontWeight: 'bold' }}>Descripción:</Typography>
                    <Paper variant="outlined" sx={{ p: 2, bgcolor: '#f9f9f9', mb: 3 }}>
                        <Typography variant="body2">{ticketSeleccionado?.descripcion}</Typography>
                    </Paper>

                    <Box sx={{ display: 'flex', gap: 6, mb: 3 }}>
                        <Box>
                            <Typography variant="subtitle2" sx={{ fontWeight: 'bold' }}>Sede:</Typography>
                            <Typography variant="body2">{ticketSeleccionado?.sede || 'SEDIF'}</Typography>
                        </Box>
                        <Box>
                            <Typography variant="subtitle2" sx={{ fontWeight: 'bold' }}>Área de origen:</Typography>
                            <Typography variant="body2">{ticketSeleccionado?.departamento || 'No especificada'}</Typography>
                        </Box>
                    </Box>

                    {/* ========================================================= */}
                    {/* JUSTIFICACIÓN (Solo aparece si el ticket está finalizado) */}
                    {/* ========================================================= */}
                    {(ticketSeleccionado?.estatus === 'CERRADO' || ticketSeleccionado?.estatus === 'RESUELTO') && ticketSeleccionado?.justificacion && (
                        <Box sx={{ mt: 1 }}>
                            <Typography variant="subtitle2" sx={{ fontWeight: 'bold', color: 'primary.main' }}>
                                Resolución del Técnico:
                            </Typography>
                            <Paper variant="outlined" sx={{ p: 2, mt: 1, bgcolor: '#f3f4f6', borderLeft: '4px solid', borderColor: 'primary.main' }}>
                                <Typography variant="body2">{ticketSeleccionado.justificacion}</Typography>
                            </Paper>
                        </Box>
                    )}

                </DialogContent>
                <DialogActions sx={{ p: 2 }}>
                    <Button onClick={handleCloseDetalle} variant="contained" sx={{ backgroundColor: '#4b5563', color: 'white', '&:hover': { backgroundColor: '#374151' }, textTransform: 'none' }}>
                        Cerrar
                    </Button>
                </DialogActions>
            </Dialog>

            {/* MODAL DE RESOLUCIÓN (AJUSTADO A COLOR INSTITUCIONAL) */}
            <Dialog open={openResolucion} onClose={handleCloseResolucion} fullWidth maxWidth="sm" disableEscapeKeyDown>
                <DialogTitle sx={{ backgroundColor: 'primary.main', color: 'white' }}>
                    Resolver Ticket #{ticketSeleccionado?.id}
                </DialogTitle>
                <DialogContent sx={{ mt: 2 }}>
                    <Typography variant="body2" color="textSecondary" sx={{ mb: 2 }}>
                        Estás a punto de marcar este ticket como resuelto. Ingresa el detalle del trabajo realizado para la bitácora del sistema.
                    </Typography>
                    
                    <Typography variant="subtitle2" sx={{ fontWeight: 'bold' }}>Justificación Técnica *</Typography>
                    <TextField
                        autoFocus
                        margin="dense"
                        fullWidth
                        multiline
                        rows={4}
                        value={justificacion}
                        onChange={(e) => setJustificacion(e.target.value)}
                        variant="outlined"
                        placeholder="Ej. Se reemplazó el cartucho de tóner negro y se reinició la cola de impresión..."
                        required
                    />
                </DialogContent>
                <DialogActions sx={{ p: 2 }}>
                    <Button onClick={() => handleCloseResolucion()} color="inherit" sx={{ textTransform: 'none' }}>
                        Cancelar
                    </Button>
                    {/* BOTÓN ACTUALIZADO */}
                    <Button onClick={handleResolverTicket} variant="contained" color="primary" sx={{ textTransform: 'none' }}>
                        Confirmar
                    </Button>
                </DialogActions>
            </Dialog>

        </Box>
    );
}