import { useState, useEffect, useContext } from 'react';
import { Box, Typography, TextField, Button, Alert, Paper, MenuItem } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import api from '../../services/api';
import { AuthContext } from '../../context/AuthContext.jsx';

import { toUpper } from '../../util/formater'; 

const COLOR_GUINDA = '#5c0a28'; 

export default function FormularioTicket() {
    const { user } = useContext(AuthContext); 
    const navigate = useNavigate();

    // 1. Estados
    const [titulo, setTitulo] = useState('');
    const [descripcion, setDescripcion] = useState('');
    const [solicitante, setSolicitante] = useState(''); 
    const [sede, setSede] = useState(''); 
    const [mensaje, setMensaje] = useState({ tipo: '', texto: '' });
    
    // ---> NUEVOS ESTADOS PARA ASIGNACIÓN
    const [usuarioSoporteId, setUsuarioSoporteId] = useState(''); 
    const [tecnicosSoporte, setTecnicosSoporte] = useState([]);

    // 2. Validación de Roles
    // Simplificamos la validación del Admin basado en tu lógica
    const esAdmin = user?.rol === 'ADMINISTRADOR' || user?.role === 'ADMINISTRADOR' || user?.rol === 'ADMIN' || user?.role === 'ADMIN';
    const esSoporte = user?.rol === 'SOPORTE' || user?.role === 'SOPORTE' || esAdmin;

    // ---> 3. NUEVO: CARGAR TÉCNICOS SI ES ADMINISTRADOR
    useEffect(() => {
        if (esAdmin) {
            const cargarSoporte = async () => {
                try {
                    // Reemplaza con la ruta base correcta de tu api axios si es diferente
                    const response = await api.get('/v1/admin/usuarios/soporte');
                    setTecnicosSoporte(response.data);
                } catch (error) {
                    console.error("Error al cargar técnicos de soporte:", error);
                }
            };
            cargarSoporte();
        }
    }, [esAdmin]);

    // 4. Lógica de envío
    const handleSubmit = async (e) => {
        e.preventDefault();
        setMensaje({ tipo: '', texto: '' });

        if (!solicitante.trim() || !titulo.trim() || !descripcion.trim()) {
            setMensaje({ tipo: 'error', texto: 'Por favor, completa todos los campos obligatorios.' });
            return;
        }

        try {
            const payload = {
                titulo,
                descripcion,
                solicitante,
                sede: esSoporte ? sede : null,
                // ---> NUEVO: Enviamos el ID del soporte (null si no se selecciona nada)
                usuarioSoporteId: usuarioSoporteId ? parseInt(usuarioSoporteId) : null
            };

            await api.post('/v1/tickets', payload);
            
            if (user?.rol === 'SOPORTE' || user?.role === 'SOPORTE') {
                navigate('/soporte/bandeja', {
                    state: { mensajeExito: '¡Ticket creado correctamente!' }
                });
            } else {
                navigate('/empleado/historial', {
                    state: { mensajeExito: '¡Ticket creado correctamente!' }
                });
            }
            
        } catch (error) {
            console.error("Error al crear:", error);
            setMensaje({ tipo: 'error', texto: 'Hubo un error al crear el ticket. Intente de nuevo.' });
        }
    };

    return (
        <Box sx={{ maxWidth: 800, mx: 'auto', p: { xs: 1, sm: 3 } }}>
            <Paper elevation={3} sx={{ p: { xs: 3, sm: 4 }, borderRadius: 3 }}>
                <Typography variant="h5" sx={{ color: COLOR_GUINDA, fontWeight: 'bold', mb: 3 }}>
                    Levantar Nuevo Ticket de Soporte
                </Typography>
                
                {mensaje.texto && (
                    <Alert severity={mensaje.tipo} sx={{ mb: 3, fontWeight: 'bold' }}>
                        {mensaje.texto}
                    </Alert>
                )}

                <form onSubmit={handleSubmit}>
                    
                    <TextField
                        fullWidth
                        label="Nombre del que tiene la falla *"
                        variant="outlined"
                        margin="normal"
                        value={solicitante}
                        onChange={(e) => setSolicitante(toUpper(e.target.value))}
                        placeholder="Escribe el nombre completo de la persona afectada"
                        required
                        inputProps={{ maxLength: 100 }}
                        helperText={`${solicitante.length}/100 caracteres`}
                    />

                    <TextField
                        fullWidth
                        label="Falla principal *"
                        variant="outlined"
                        margin="normal"
                        value={titulo}
                        onChange={(e) => setTitulo(toUpper(e.target.value))}
                        placeholder="Ej. La impresora no se conecta a la red / Pantalla en negro"
                        required
                        inputProps={{ maxLength: 100 }}
                        helperText={`${titulo.length}/100 caracteres`}
                    />

                    {esSoporte && (
                        <TextField
                            fullWidth
                            label="Sede *"
                            variant="outlined"
                            margin="normal"
                            value={sede}
                            onChange={(e) => setSede(toUpper(e.target.value))}
                            required={esSoporte}
                            inputProps={{ maxLength: 50 }}
                        />
                    )}

                    <TextField
                        fullWidth
                        label="Favor de describir la falla *"
                        variant="outlined"
                        margin="normal"
                        multiline
                        rows={4}
                        value={descripcion}
                        onChange={(e) => setDescripcion(toUpper(e.target.value))}
                        placeholder="Describe detalladamente qué acciones causan el problema o qué mensajes de error aparecen en pantalla..."
                        required
                        inputProps={{ maxLength: 500 }}
                        helperText={`${descripcion.length}/500 caracteres`}
                    />

                    {/* ---> NUEVO SELECTOR SÓLO PARA ADMINISTRADORES <--- */}
                    {esAdmin && (
                        <TextField
                            select
                            fullWidth
                            label="Asignar directamente a Soporte (Opcional)"
                            variant="outlined"
                            margin="normal"
                            value={usuarioSoporteId}
                            onChange={(e) => setUsuarioSoporteId(e.target.value)}
                        >
                            <MenuItem value="">
                                <em>-- Selección Automática (Balanceador) --</em>
                            </MenuItem>
                            {tecnicosSoporte.map((tecnico) => (
                                <MenuItem key={tecnico.id} value={tecnico.id}>
                                    {tecnico.nombre} {tecnico.disponibleSoporte ? '(Disponible)' : '(Ocupado)'}
                                </MenuItem>
                            ))}
                        </TextField>
                    )}
                    
                    <Box sx={{ mt: 4, display: 'flex', justifyContent: 'flex-end' }}>
                        <Button
                            type="submit"
                            variant="contained"
                            sx={{ 
                                px: 4, 
                                py: 1.5, 
                                fontSize: '1rem', 
                                bgcolor: COLOR_GUINDA,
                                '&:hover': { bgcolor: '#4a0820' }
                            }}
                        >
                            Enviar Ticket
                        </Button>
                    </Box>
                </form>
            </Paper>
        </Box>
    );
}