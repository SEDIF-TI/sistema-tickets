import { useState, useContext } from 'react';
import { Box, Typography, TextField, Button, Alert, Paper } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import api from '../../services/api';
import { AuthContext } from '../../context/AuthContext.jsx';

export default function FormularioTicket() {
    // 1. Contexto y Navegación
    const { user } = useContext(AuthContext); // Obtenemos el usuario para verificar su rol
    const navigate = useNavigate();

    // 2. Estados unificados
    const [titulo, setTitulo] = useState('');
    const [descripcion, setDescripcion] = useState('');
    const [sede, setSede] = useState(''); // Nuevo estado para la sede
    const [mensaje, setMensaje] = useState({ tipo: '', texto: '' });

    // 3. Validación de Rol (Se muestra la sede si es SOPORTE o ADMIN)
    const esSoporte = user?.rol === 'SOPORTE' || user?.rol === 'ADMIN';

    // 4. Lógica de envío
    const handleSubmit = async (e) => {
        e.preventDefault();
        setMensaje({ tipo: '', texto: '' });

        try {
            // Construimos el objeto a enviar. Si no es soporte, sede va como null.
            const payload = {
                titulo,
                descripcion,
                sede: esSoporte ? sede : null
            };

            await api.post('/v1/tickets', payload);
            
            // Redirección condicional enviando el mensaje de éxito en el estado de la ruta
            if (user?.rol === 'SOPORTE') {
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

    // 5. Renderizado del Formulario (Con tu diseño institucional)
    return (
        <Box sx={{ maxWidth: 800, mx: 'auto', p: { xs: 1, sm: 3 } }}>
            <Paper elevation={2} sx={{ p: { xs: 3, sm: 4 }, borderRadius: 3 }}>
                <Typography variant="h5" sx={{ color: '#2c3e50', fontWeight: 'bold', mb: 3 }}>
                    Levantar Nuevo Ticket
                </Typography>
                
                {mensaje.texto && (
                    <Alert severity={mensaje.tipo} sx={{ mb: 3 }}>
                        {mensaje.texto}
                    </Alert>
                )}

                <form onSubmit={handleSubmit}>
                    <TextField
                        fullWidth
                        label="Título *"
                        variant="outlined"
                        margin="normal"
                        value={titulo}
                        onChange={(e) => setTitulo(e.target.value)}
                        required
                    />

                    {/* RENDERIZACIÓN CONDICIONAL: Solo aparece si el rol es Soporte/Admin */}
                    {esSoporte && (
                        <TextField
                            fullWidth
                            label="Sede *"
                            variant="outlined"
                            margin="normal"
                            value={sede}
                            onChange={(e) => setSede(e.target.value)}
                            required={esSoporte}
                        />
                    )}

                    <TextField
                        fullWidth
                        label="Descripción *"
                        variant="outlined"
                        margin="normal"
                        multiline
                        rows={4}
                        value={descripcion}
                        onChange={(e) => setDescripcion(e.target.value)}
                        required
                    />
                    
                    <Box sx={{ mt: 4, display: 'flex', justifyContent: 'flex-end' }}>
                        {/* El botón toma tu color #5c0a28 automáticamente por el color="primary" del theme.js */}
                        <Button
                            type="submit"
                            variant="contained"
                            color="primary"
                            sx={{ px: 4, py: 1.5, fontSize: '1rem' }}
                        >
                            Enviar Ticket
                        </Button>
                    </Box>
                </form>
            </Paper>
        </Box>
    );
}