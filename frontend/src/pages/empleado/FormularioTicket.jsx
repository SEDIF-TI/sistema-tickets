import { useState, useContext } from 'react';
import { Box, Typography, TextField, Button, Alert, Paper } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import api from '../../services/api';
import { AuthContext } from '../../context/AuthContext.jsx';

const COLOR_GUINDA = '#5c0a28'; // Sincronizado con tu tema institucional

export default function FormularioTicket() {
    // 1. Contexto y Navegación
    const { user } = useContext(AuthContext); 
    const navigate = useNavigate();

    // 2. Estados unificados (Incluyendo el nuevo campo Solicitante)
    const [titulo, setTitulo] = useState('');
    const [descripcion, setDescripcion] = useState('');
    const [solicitante, setSolicitante] = useState(''); // <-- NUEVO ESTADO
    const [sede, setSede] = useState(''); 
    const [mensaje, setMensaje] = useState({ tipo: '', texto: '' });

    // 3. Validación de Rol (Se muestra la sede si es SOPORTE o ADMIN)
    const esSoporte = user?.rol === 'SOPORTE' || user?.rol === 'ADMIN' || user?.role === 'ADMINISTRADOR';

    // 4. Lógica de envío
    const handleSubmit = async (e) => {
        e.preventDefault();
        setMensaje({ tipo: '', texto: '' });

        if (!solicitante.trim() || !titulo.trim() || !descripcion.trim()) {
            setMensaje({ tipo: 'error', texto: 'Por favor, completa todos los campos obligatorios.' });
            return;
        }

        try {
            // Construimos el objeto respetando los nombres que el backend ya mapea
            const payload = {
                titulo,
                descripcion,
                solicitante, // <-- Enviamos el nombre de la persona que tiene la falla física
                sede: esSoporte ? sede : null
            };

            await api.post('/v1/tickets', payload);
            
            // Redirección condicional con el mensaje de éxito en el estado de la ruta
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
                    
                    {/* 1. NUEVO CAMPO: SOLICITANTE */}
                    <TextField
                        fullWidth
                        label="Nombre del que tiene la falla *"
                        variant="outlined"
                        margin="normal"
                        value={solicitante}
                        onChange={(e) => setSolicitante(e.target.value)}
                        placeholder="Escribe el nombre completo de la persona afectada"
                        required
                        inputProps={{ maxLength: 100 }} // Limitación de caracteres
                        helperText={`${solicitante.length}/100 caracteres`}
                    />

                    {/* 2. CAMPO MODIFICADO: FALLA PRINCIPAL (Título) */}
                    <TextField
                        fullWidth
                        label="Falla principal *"
                        variant="outlined"
                        margin="normal"
                        value={titulo}
                        onChange={(e) => setTitulo(e.target.value)}
                        placeholder="Ej. La impresora no se conecta a la red / Pantalla en negro"
                        required
                        inputProps={{ maxLength: 100 }} // Limitación de caracteres
                        helperText={`${titulo.length}/100 caracteres`}
                    />

                    {/* RENDERIZACIÓN CONDICIONAL: Sede */}
                    {esSoporte && (
                        <TextField
                            fullWidth
                            label="Sede *"
                            variant="outlined"
                            margin="normal"
                            value={sede}
                            onChange={(e) => setSede(e.target.value)}
                            required={esSoporte}
                            inputProps={{ maxLength: 50 }}
                        />
                    )}

                    {/* 3. CAMPO MODIFICADO: DESCRIPCIÓN DE LA FALLA */}
                    <TextField
                        fullWidth
                        label="Favor de describir la falla *"
                        variant="outlined"
                        margin="normal"
                        multiline
                        rows={4}
                        value={descripcion}
                        onChange={(e) => setDescripcion(e.target.value)}
                        placeholder="Describe detalladamente qué acciones causan el problema o qué mensajes de error aparecen en pantalla..."
                        required
                        inputProps={{ maxLength: 500 }} // Limitación de caracteres
                        helperText={`${descripcion.length}/500 caracteres`}
                    />
                    
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