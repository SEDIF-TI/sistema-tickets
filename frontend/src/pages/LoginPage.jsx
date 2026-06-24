import { useNavigate } from 'react-router-dom';
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
    const [identificador, setIdentificador] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    
    const { login } = useContext(AuthContext);
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault(); 
        setError(''); 

        if (!identificador.trim() || !password.trim()) {
            setError('Por favor, ingresa tu usuario/correo y contraseña.');
            return;
        }

       try {
            await login(identificador, password);
            console.log("¡Login exitoso!");
            
            // ¡EL CAMBIO CRÍTICO ESTÁ AQUÍ!
            // Ya no buscamos vistas manualmente. Mandamos a la raíz y dejamos que App.jsx tome el control.
            navigate('/', { replace: true }); 
            
        } catch (err) {
            setError('Credenciales inválidas o error de conexión.');
        }
    };

    return (
        <Container component="main" maxWidth="xs" sx={{ px: 2 }}> {/* px: 2 da un respiro en celulares */}
            <Box sx={{ mt: 8, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                <Paper elevation={3} sx={{ p: { xs: 3, md: 4 }, width: '100%', borderRadius: 3 }}>
                    <Typography component="h1" variant="h5" align="center" gutterBottom>
                        Sistema de Tickets
                    </Typography>
                    <Typography variant="body2" align="center" color="textSecondary" sx={{ mb: 3 }}>
                        Ingresa tus credenciales para continuar
                    </Typography>

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