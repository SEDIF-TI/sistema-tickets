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
 * Pantalla de inicio de sesión.
 *
 * Correcciones respecto a la versión anterior:
 *  - El botón no tenía estado de carga: se podía pulsar repetidamente. Con el
 *    límite de 5 intentos por minuto del backend, un doble clic nervioso
 *    bastaba para que el usuario se bloqueara a sí mismo.
 *  - El mensaje de error era siempre el mismo ("Credenciales inválidas o
 *    error de conexión"), sin distinguir una contraseña equivocada de un
 *    servidor caído o de un bloqueo por intentos.
 *  - Se añadió el conmutador para ver la contraseña, imprescindible al
 *    teclear a mano una clave temporal de 14 caracteres.
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

                    {/* Identidad institucional. El logotipo original es oscuro,
                        así que aquí se muestra tal cual sobre fondo blanco. */}
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
                            InputProps={{
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
