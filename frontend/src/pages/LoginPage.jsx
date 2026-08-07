import { useNavigate } from 'react-router-dom';
import { useState, useContext } from 'react';
import {
    Container, Box, TextField, Button, Typography, Alert, Paper,
    CircularProgress, InputAdornment, IconButton
} from '@mui/material';
import Visibility from '@mui/icons-material/Visibility';
import VisibilityOff from '@mui/icons-material/VisibilityOff';

import { AuthContext } from '../context/AuthContext';
import logoPuebla from '../assets/logo-puebla.png';

/**
 * Pantalla de inicio de sesión, única ruta pública del sistema.
 *
 * El campo de identificador acepta indistintamente el usuario o el correo. Al
 * enviar, `login` del AuthContext llama al backend, guarda el JWT devuelto y
 * deja la sesión disponible para el resto de la aplicación; a partir de ahí
 * basta con navegar a la raíz.
 *
 * El backend limita los intentos a cinco por minuto, así que el botón se
 * deshabilita mientras la petición viaja: un doble clic bastaría para que el
 * usuario se bloqueara a sí mismo. Los tres motivos de fallo —bloqueo por
 * intentos, servidor inalcanzable y credenciales incorrectas— se distinguen en
 * el mensaje, porque la acción del usuario es distinta en cada caso.
 *
 * El conmutador de visibilidad de la contraseña es necesario para teclear a
 * mano las claves temporales de 14 caracteres que se entregan en el alta.
 */
export default function LoginPage() {
    const [identificador, setIdentificador] = useState('');
    const [password, setPassword] = useState('');
    const [verPassword, setVerPassword] = useState(false);
    const [error, setError] = useState('');
    const [cargando, setCargando] = useState(false);

    const { login } = useContext(AuthContext);
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        if (!identificador.trim() || !password.trim()) {
            setError('Escribe tu usuario o correo y tu contraseña.');
            return;
        }

        setCargando(true);
        try {
            await login(identificador, password);
            // App.jsx decide el destino según el rol y si la contraseña es
            // temporal, así que basta con ir a la raíz.
            navigate('/', { replace: true });
        } catch (err) {
            if (err.response?.status === 429) {
                setError('Demasiados intentos fallidos. Espera unos minutos antes de volver a intentarlo.');
            } else if (!err.response) {
                setError('No hay conexión con el servidor. Verifica tu red e inténtalo de nuevo.');
            } else {
                // Mensaje deliberadamente genérico: no revela si la cuenta existe.
                setError(err.mensaje || 'Credenciales inválidas. Verifica tus datos.');
            }
        } finally {
            setCargando(false);
        }
    };

    return (
        <Box
            sx={{
                minHeight: '100vh',
                display: 'flex',
                alignItems: 'center',
                bgcolor: 'background.default',
                py: 4,
            }}
        >
            <Container component="main" maxWidth="sm" sx={{ px: 2 }}>
                <Paper elevation={2} sx={{ p: { xs: 3, sm: 5 } }}>

                    {/* El logotipo institucional es oscuro y se muestra tal
                        cual sobre el fondo claro del formulario. */}
                    <Box sx={{ display: 'flex', justifyContent: 'center', mb: 4 }}>
                        <Box
                            component="img"
                            src={logoPuebla}
                            alt="Gobierno del Estado de Puebla"
                            sx={{ width: '100%', maxWidth: 340, height: 'auto' }}
                        />
                    </Box>

                    <Box sx={{ textAlign: 'center', mb: 4 }}>
                        <Typography component="h1" variant="h4" gutterBottom>
                            Sistema de Tickets
                        </Typography>
                        <Typography variant="body1" color="text.secondary">
                            Ingresa tus credenciales para continuar
                        </Typography>
                    </Box>

                    {/* role="alert" hace que el lector de pantalla lo anuncie
                        en cuanto aparece, sin tener que buscarlo. */}
                    {error && (
                        <Alert severity="error" role="alert" sx={{ mb: 3 }}>
                            {error}
                        </Alert>
                    )}

                    <Box component="form" onSubmit={handleSubmit} noValidate>
                        <TextField
                            margin="normal"
                            required
                            fullWidth
                            id="identificador"
                            label="Correo electrónico o usuario"
                            name="identificador"
                            autoComplete="username"
                            autoFocus
                            value={identificador}
                            onChange={(e) => setIdentificador(e.target.value)}
                            disabled={cargando}
                        />

                        <TextField
                            margin="normal"
                            required
                            fullWidth
                            name="password"
                            label="Contraseña"
                            type={verPassword ? 'text' : 'password'}
                            id="password"
                            autoComplete="current-password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            disabled={cargando}
                            // El botón de visibilidad va en `slotProps.input`, que es la
                            // vía de MUI 9 para llegar al componente interno del input.
                            slotProps={{
                                input: {
                                    endAdornment: (
                                        <InputAdornment position="end">
                                            <IconButton
                                                onClick={() => setVerPassword((v) => !v)}
                                                edge="end"
                                                size="small"
                                                aria-label={
                                                    verPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'
                                                }
                                            >
                                                {verPassword ? <VisibilityOff /> : <Visibility />}
                                            </IconButton>
                                        </InputAdornment>
                                    ),
                                },
                            }}
                        />

                        <Button
                            type="submit"
                            fullWidth
                            size="large"
                            variant="contained"
                            disabled={cargando}
                            sx={{ mt: 4 }}
                        >
                            {cargando ? (
                                <>
                                    <CircularProgress size={20} color="inherit" sx={{ mr: 1.5 }} />
                                    Verificando…
                                </>
                            ) : (
                                'Iniciar sesión'
                            )}
                        </Button>
                    </Box>
                </Paper>

                <Typography
                    variant="caption"
                    color="text.secondary"
                    align="center"
                    sx={{ display: 'block', mt: 3 }}
                >
                    SEDIF · Sistema de gestión de tickets
                </Typography>
            </Container>
        </Box>
    );
}
