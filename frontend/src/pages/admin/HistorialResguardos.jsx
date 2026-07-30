import { useState, useEffect, useContext } from 'react';
import { 
    Box, Typography, Paper, Table, TableBody, TableCell, 
    TableContainer, TableHead, TableRow, Button, Chip, 
    Snackbar, Alert 
} from '@mui/material';
import { useLocation } from 'react-router-dom';
import api from '../../services/api';
import { AuthContext } from '../../context/AuthContext.jsx';

// Importaciones para WebSocket (Asegúrate de tener instalados 'sockjs-client' y '@stomp/stompjs' o ajusta a tu librería actual)
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

const COLOR_GUINDA = '#5c0a28';

export default function HistorialResguardos() {
    const { user } = useContext(AuthContext);
    const location = useLocation();
    
    // Estados
    const [resguardos, setResguardos] = useState([]);
    const [notificacion, setNotificacion] = useState({ open: false, mensaje: '', tipo: 'info' });

    // 1. Cargar la lista inicial de resguardos
    const cargarResguardos = async () => {
        try {
            const response = await api.get('/v1/resguardos');
            setResguardos(response.data);
        } catch (error) {
            console.error("Error al cargar resguardos:", error);
        }
    };

    useEffect(() => {
        cargarResguardos();
        
        // Si venimos de crear un resguardo, mostramos el mensaje de éxito
        if (location.state?.mensajeExito) {
            setNotificacion({ open: true, mensaje: location.state.mensajeExito, tipo: 'success' });
            window.history.replaceState({}, document.title); // Limpiamos el state
        }
    }, [location]);

    // 2. Conexión WebSocket para alertas en tiempo real (Cron Job 9:00 AM)
    useEffect(() => {
        // Ajusta la URL '/ws' según cómo tengas configurado tu endpoint de WebSockets en Spring Boot
        const socket = new SockJS('http://localhost:8080/ws'); 
        const stompClient = Stomp.over(socket);
        
        // Desactivamos logs en producción
        stompClient.debug = () => {}; 

        stompClient.connect({}, () => {
            stompClient.subscribe('/topic/alertas-resguardos', (mensaje) => {
                const resguardoVencido = JSON.parse(mensaje.body);
                
                // VALIDACIÓN CLAVE: Solo mostramos la alerta si el usuario logueado creó este resguardo
                if (user && resguardoVencido.usuarioCreadorId === user.id) {
                    setNotificacion({
                        open: true,
                        mensaje: `⚠️ URGENTE: El resguardo de ${resguardoVencido.equipoNombre} asignado a ${resguardoVencido.solicitanteNombre} ha VENCIDO.`,
                        tipo: 'error'
                    });
                    
                    // Actualizamos la tabla dinámicamente sin recargar la página
                    setResguardos((prev) => prev.map(r => 
                        r.id === resguardoVencido.id ? { ...r, estado: 'VENCIDO' } : r
                    ));
                }
            });
        });

        return () => {
            if (stompClient) stompClient.disconnect();
        };
    }, [user]);

    // 3. Acción para marcar como DEVUELTO
    const handleDevolucion = async (id) => {
        if (!window.confirm('¿Confirmas que el equipo ha sido devuelto físicamente?')) return;

        try {
            await api.put(`/v1/resguardos/${id}/devolucion`);
            setNotificacion({ open: true, mensaje: 'Equipo devuelto correctamente.', tipo: 'success' });
            
            // Actualizamos el estado localmente
            setResguardos((prev) => prev.map(r => 
                r.id === id ? { ...r, estado: 'DEVUELTO' } : r
            ));
        } catch (error) {
            console.error("Error en devolución:", error);
            setNotificacion({ open: true, mensaje: 'Error al procesar la devolución.', tipo: 'error' });
        }
    };

    // 4. Utilidades de Formato
    const formatearFecha = (fecha) => {
        if (!fecha) return 'PERMANENTE';
        const d = new Date(fecha);
        return d.toLocaleDateString('es-MX', { year: 'numeric', month: '2-digit', day: '2-digit' });
    };

    const getChipColor = (estado) => {
        switch (estado) {
            case 'ENTREGADO': return 'primary';
            case 'VENCIDO': return 'error';
            case 'DEVUELTO': return 'success';
            default: return 'default';
        }
    };

    return (
        <Box sx={{ maxWidth: 1200, mx: 'auto', p: { xs: 1, sm: 3 } }}>
            <Typography variant="h5" sx={{ color: COLOR_GUINDA, fontWeight: 'bold', mb: 3 }}>
                Historial y Trazabilidad de Resguardos
            </Typography>

            <TableContainer component={Paper} elevation={3} sx={{ borderRadius: 3 }}>
                <Table>
                    <TableHead sx={{ bgcolor: COLOR_GUINDA }}>
                        <TableRow>
                            <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Solicitante</TableCell>
                            <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Equipo</TableCell>
                            <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Vencimiento</TableCell>
                            <TableCell sx={{ color: 'white', fontWeight: 'bold', align: 'center' }}>Estado</TableCell>
                            <TableCell sx={{ color: 'white', fontWeight: 'bold', align: 'center' }}>Acciones</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {resguardos.length === 0 ? (
                            <TableRow>
                                <TableCell colSpan={5} align="center">
                                    No hay resguardos registrados.
                                </TableCell>
                            </TableRow>
                        ) : (
                            resguardos.map((r) => (
                                <TableRow key={r.id} hover>
                                    <TableCell>
                                        <Typography variant="body2" fontWeight="bold">
                                            {r.solicitanteNombre}
                                        </Typography>
                                        <Typography variant="caption" color="textSecondary">
                                            No. {r.solicitanteNumero}
                                        </Typography>
                                    </TableCell>
                                    <TableCell>
                                        <Typography variant="body2">{r.equipoNombre}</Typography>
                                        <Typography variant="caption" color="textSecondary">
                                            SN: {r.numeroSerie}
                                        </Typography>
                                    </TableCell>
                                    <TableCell>
                                        <Typography 
                                            variant="body2" 
                                            fontWeight={r.fechaVencimiento ? 'normal' : 'bold'}
                                        >
                                            {formatearFecha(r.fechaVencimiento)}
                                        </Typography>
                                    </TableCell>
                                    <TableCell align="center">
                                        <Chip 
                                            label={r.estado} 
                                            color={getChipColor(r.estado)} 
                                            size="small" 
                                            sx={{ fontWeight: 'bold' }} 
                                        />
                                    </TableCell>
                                    <TableCell align="center">
                                        {r.estado !== 'DEVUELTO' ? (
                                            <Button 
                                                variant="outlined" 
                                                color="success" 
                                                size="small"
                                                
                                                onClick={() => handleDevolucion(r.id)}
                                            >
                                                Devolver
                                            </Button>
                                        ) : (
                                            <Typography variant="caption" color="textSecondary">
                                                Finalizado
                                            </Typography>
                                        )}
                                    </TableCell>
                                </TableRow>
                            ))
                        )}
                    </TableBody>
                </Table>
            </TableContainer>

            {/* Alerta flotante para mensajes de éxito y notificaciones de WebSocket */}
            <Snackbar
                open={notificacion.open}
                autoHideDuration={6000}
                onClose={() => setNotificacion({ ...notificacion, open: false })}
                anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
            >
                <Alert 
                    onClose={() => setNotificacion({ ...notificacion, open: false })} 
                    severity={notificacion.tipo} 
                    sx={{ width: '100%', fontWeight: 'bold' }}
                >
                    {notificacion.mensaje}
                </Alert>
            </Snackbar>
        </Box>
    );
}