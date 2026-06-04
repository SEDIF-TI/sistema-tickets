import { useState, useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { 
    Container, 
    Box, 
    TextField, 
    Button, 
    Typography, 
    Alert, 
    Paper 
} from '@mui/material';

export default function LoginPage() {
    // Estados para controlar los inputs y errores
    const [identificador, setIdentificador] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    
    // Traemos la función login de nuestro contexto
    const { login } = useContext(AuthContext);

    const handleSubmit = async (e) => {
        e.preventDefault(); // Evita que la página se recargue
        setError(''); // Limpiamos errores previos

        // 1. Validación en el Frontend
        if (!identificador.trim() || !password.trim()) {
            setError('Por favor, ingresa tu usuario/correo y contraseña.');
            return;
        }

        // 2. Intento de inicio de sesión
        try {
            await login(identificador, password);
            // Si el login es exitoso, aquí luego agregaremos la redirección al Dashboard
            console.log("¡Login exitoso!");
        } catch (err) {
            // Si el backend devuelve un 401 o 403, caemos aquí
            setError('Credenciales inválidas. Verifica tu información.');
        }
    };

    return (
        <Container component="main" maxWidth="xs">
            <Box
                sx={{
                    marginTop: 8,
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                }}
            >
                <Paper elevation={3} sx={{ padding: 4, width: '100%', borderRadius: 2 }}>
                    <Typography component="h1" variant="h5" align="center" gutterBottom>
                        Sistema de Tickets
                    </Typography>
                    <Typography variant="body2" align="center" color="textSecondary" sx={{ mb: 3 }}>
                        Ingresa tus credenciales para continuar
                    </Typography>

                    {/* Alerta de Error de MUI */}
                    {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

                    <Box component="form" onSubmit={handleSubmit} noValidate>
                        <TextField
                            margin="normal"
                            required
                            fullWidth
                            id="identificador"
                            label="Correo electrónico o Usuario"
                            name="identificador"
                            autoComplete="email"
                            autoFocus
                            value={identificador}
                            onChange={(e) => setIdentificador(e.target.value)}
                        />
                        <TextField
                            margin="normal"
                            required
                            fullWidth
                            name="password"
                            label="Contraseña"
                            type="password"
                            id="password"
                            autoComplete="current-password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                        />
                        <Button
                            type="submit"
                            fullWidth
                            variant="contained"
                            color="primary"
                            sx={{ mt: 3, mb: 2, py: 1.5 }}
                        >
                            Iniciar Sesión
                        </Button>
                    </Box>
                </Paper>
            </Box>
        </Container>
    );
}