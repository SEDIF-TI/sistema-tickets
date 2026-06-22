import React, { useEffect, useState, useContext } from 'react';
import { 
    Typography, Box, Chip, Paper, Table, TableBody, 
    TableCell, TableContainer, TableHead, TableRow, Button,
    Dialog, DialogTitle, DialogContent, DialogActions, TextField, IconButton,
    TablePagination, InputAdornment, CircularProgress, Tooltip
} from '@mui/material';

import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import VisibilityIcon from '@mui/icons-material/Visibility';
import SearchIcon from '@mui/icons-material/Search';
import DirectionsRunIcon from '@mui/icons-material/DirectionsRun';
import AssignmentIcon from '@mui/icons-material/Assignment';

import { useWebSocket } from '../../context/WebSocketContext.jsx';
import { AuthContext } from '../../context/AuthContext.jsx'; 
import api from '../../services/api';

const COLOR_GUINDA = '#801A36';

export default function PanelSoporte() {
    const { stompClient, isConnected } = useWebSocket();
    const { user } = useContext(AuthContext); 
    
    const [tickets, setTickets] = useState([]);
    const [cargando, setCargando] = useState(true);

    const [openDetalle, setOpenDetalle] = useState(false);
    const [openResolucion, setOpenResolucion] = useState(false);
    
    const [justificacion, setJustificacion] = useState('');
    const [ticketSeleccionado, setTicketSeleccionado] = useState(null);

    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(10);
    const [busqueda, setBusqueda] = useState('');

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

    useEffect(() => {
        cargarTicketsHistoricos();
    }, []);

    useEffect(() => {
        if (isConnected && stompClient && stompClient.connected && user) {
            try {
                const suscripcion = stompClient.subscribe('/topic/tickets-soporte', (mensaje) => {
                    const nuevoTicket = JSON.parse(mensaje.body);
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
    }, [isConnected, stompClient, user]); 

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

    const formatearFecha = (fecha) => {
        if (!fecha) return '--/--/----';
        const d = new Date(fecha);
        const pad = (n) => n.toString().padStart(2, '0');
        return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear().toString().slice(-2)} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
    };

    const getColorEstatus = (estatus) => {
        const estado = (estatus || '').toUpperCase();
        if (estado === 'CERRADO' || estado === 'RESUELTO') return 'default';
        if (estado === 'EN PROCESO') return 'info';
        if (estado === 'ABIERTO' || estado === 'ASIGNADO') return 'warning';
        return 'primary';
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
        <Box sx={{ p: { xs: 2, md: 4 }, backgroundColor: '#f4f7f6', minHeight: '100vh', width: '100%' }}>
            
            {/* ENCABEZADO */}
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
                <Typography variant="h4" fontWeight="bold" sx={{ color: COLOR_GUINDA, display: 'flex', alignItems: 'center', gap: 1 }}>
                    <AssignmentIcon fontSize="large" />
                    Tickets Asignados
                </Typography>
            </Box>

            <Paper elevation={3} sx={{ borderRadius: 2, overflow: 'hidden', mb: 4 }}>
                
                {/* BARRA DE BÚSQUEDA */}
                <Box sx={{ p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center', bgcolor: '#ffffff', borderBottom: '1px solid #e0e0e0' }}>
                    <Typography variant="body2" color="textSecondary">
                        Mostrando registros de soporte
                    </Typography>
                    <TextField
                        variant="outlined"
                        size="small"
                        placeholder="Buscar ticket..."
                        value={busqueda}
                        onChange={(e) => {
                            setBusqueda(e.target.value);
                            setPage(0);
                        }}
                        sx={{ width: { xs: '100%', sm: '300px' } }}
                        InputProps={{
                            startAdornment: (
                                <InputAdornment position="start">
                                    <SearchIcon color="action" />
                                </InputAdornment>
                            ),
                        }}
                    />
                </Box>

                {/* TABLA DE DATOS */}
                <TableContainer>
                    <Table sx={{ minWidth: 800 }}>
                        <TableHead>
                            <TableRow sx={{ bgcolor: COLOR_GUINDA }}>
                                <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>ID</TableCell>
                                <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Solicitante</TableCell>
                                <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Departamento</TableCell>
                                <TableCell sx={{ color: 'white', fontWeight: 'bold', textAlign: 'center' }}>Detalles</TableCell>
                                <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Inicio</TableCell>
                                <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Fin</TableCell>
                                <TableCell sx={{ color: 'white', fontWeight: 'bold', textAlign: 'center' }}>Estatus</TableCell>
                                <TableCell sx={{ color: 'white', fontWeight: 'bold', textAlign: 'center' }}>Acciones</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {cargando ? (
                                <TableRow>
                                    <TableCell colSpan={8} align="center" sx={{ py: 6 }}>
                                        <CircularProgress sx={{ color: COLOR_GUINDA }} />
                                    </TableCell>
                                </TableRow>
                            ) : ticketsFiltrados.length === 0 ? (
                                <TableRow>
                                    <TableCell colSpan={8} align="center" sx={{ py: 6 }}>
                                        <Typography color="textSecondary">No se encontraron tickets con esos criterios.</Typography>
                                    </TableCell>
                                </TableRow>
                            ) : (
                                ticketsPaginados.map((ticket, index) => {
                                    const numeroFila = page * rowsPerPage + index + 1;
                                    const estatusLabel = ticket.estado || ticket.estatus || 'ABIERTO';

                                    return (
                                        <TableRow key={ticket.id} hover sx={{ transition: '0.2s', '&:hover': { bgcolor: '#f9f9f9' } }}>
                                            <TableCell sx={{ fontWeight: 'bold' }}>{numeroFila}</TableCell>
                                            
                                            <TableCell sx={{ fontWeight: '500' }}>{ticket.solicitante || 'Usuario'}</TableCell>
                                            <TableCell>{ticket.departamento || 'Área'}</TableCell>
                                            
                                            <TableCell align="center">
                                                <Button 
                                                    variant="outlined" 
                                                    size="small" 
                                                    onClick={() => handleOpenDetalle(ticket)}
                                                    startIcon={<VisibilityIcon />} 
                                                    sx={{ color: COLOR_GUINDA, borderColor: COLOR_GUINDA, textTransform: 'none', borderRadius: 2 }}
                                                >
                                                    Ver Ticket
                                                </Button>
                                            </TableCell>

                                            <TableCell sx={{ whiteSpace: 'nowrap', fontSize: '0.85rem' }}>{formatearFecha(ticket.fechaCreacion)}</TableCell>
                                            <TableCell sx={{ whiteSpace: 'nowrap', fontSize: '0.85rem' }}>{formatearFecha(ticket.fechaFin)}</TableCell>
                                            
                                            <TableCell align="center">
                                                <Chip 
                                                    label={estatusLabel} 
                                                    color={getColorEstatus(estatusLabel)} 
                                                    size="small" 
                                                    sx={{ fontWeight: 'bold', minWidth: '90px' }} 
                                                />
                                            </TableCell>
                                            
                                            <TableCell align="center">
                                                {(estatusLabel === 'ASIGNADO' || estatusLabel === 'ABIERTO') && (
                                                    <Tooltip title="Voy en camino">
                                                        <IconButton onClick={() => handleAtenderTicket(ticket)} sx={{ color: '#f39c12' }}>
                                                            <DirectionsRunIcon />
                                                        </IconButton>
                                                    </Tooltip>
                                                )}
                                                {estatusLabel === 'EN PROCESO' && (
                                                    <Tooltip title="Resolver Ticket">
                                                        <IconButton onClick={() => handleOpenResolucion(ticket)} sx={{ color: '#2ecc71' }}>
                                                            <CheckCircleIcon />
                                                        </IconButton>
                                                    </Tooltip>
                                                )}
                                                {(estatusLabel === 'RESUELTO' || estatusLabel === 'CERRADO') && (
                                                    <Typography variant="body2" color="textSecondary" sx={{ fontWeight: 'bold' }}>
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
                />
            </Paper>

            {/* MODAL DE DETALLES */}
            <Dialog open={openDetalle} onClose={handleCloseDetalle} fullWidth maxWidth="sm">
                <DialogTitle sx={{ backgroundColor: COLOR_GUINDA, color: 'white' }}>
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

                    {(ticketSeleccionado?.estatus === 'CERRADO' || ticketSeleccionado?.estatus === 'RESUELTO') && ticketSeleccionado?.justificacion && (
                        <Box sx={{ mt: 1 }}>
                            <Typography variant="subtitle2" sx={{ fontWeight: 'bold', color: COLOR_GUINDA }}>
                                Resolución del Técnico:
                            </Typography>
                            <Paper variant="outlined" sx={{ p: 2, mt: 1, bgcolor: '#f3f4f6', borderLeft: '4px solid', borderColor: COLOR_GUINDA }}>
                                <Typography variant="body2">{ticketSeleccionado.justificacion}</Typography>
                            </Paper>
                        </Box>
                    )}
                </DialogContent>
                <DialogActions sx={{ p: 2 }}>
                    <Button onClick={handleCloseDetalle} variant="outlined" sx={{ color: COLOR_GUINDA, borderColor: COLOR_GUINDA, textTransform: 'none' }}>
                        Cerrar
                    </Button>
                </DialogActions>
            </Dialog>

            {/* MODAL DE RESOLUCIÓN */}
            <Dialog open={openResolucion} onClose={handleCloseResolucion} fullWidth maxWidth="sm" disableEscapeKeyDown>
                <DialogTitle sx={{ backgroundColor: COLOR_GUINDA, color: 'white' }}>
                    Resolver Ticket #{ticketSeleccionado?.id}
                </DialogTitle>
                <DialogContent sx={{ mt: 2 }}>
                    <Typography variant="body2" color="textSecondary" sx={{ mb: 2 }}>
                        Estás a punto de marcar este ticket como resuelto. Ingresa el detalle del trabajo realizado.
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
                        placeholder="Ej. Se reemplazó el cartucho de tóner negro..."
                        required
                    />
                </DialogContent>
                <DialogActions sx={{ p: 2 }}>
                    <Button onClick={handleCloseResolucion} color="inherit" sx={{ textTransform: 'none' }}>Cancelar</Button>
                    <Button onClick={handleResolverTicket} variant="contained" sx={{ bgcolor: COLOR_GUINDA, textTransform: 'none' }}>Confirmar</Button>
                </DialogActions>
            </Dialog>

        </Box>
    );
}