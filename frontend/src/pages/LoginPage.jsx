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

// 1. Importar el logo
import logoPuebla from '../assets/logo-puebla.png'; 

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
            navigate('/', { replace: true }); 
        } catch (err) {
            setError('Credenciales inválidas o error de conexión.');
        }
    };

    return (
        <Container component="main" maxWidth="sm" sx={{ px: 2 }}> 
            <Box sx={{ mt: 8, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                <Paper elevation={3} sx={{ p: { xs: 3, md: 4 }, width: '100%', borderRadius: 3 }}>
                    
                    {/* 2. Modificamos el contenedor y la imagen para que use todo el ancho */}
                    <Box sx={{ display: 'flex', justifyContent: 'center', mb: 4, mt: 1, px: 2 }}>
                        <img 
                            src={logoPuebla} 
                            alt="Gobierno de Puebla" 
                            style={{ 
                                width: '100%',    // Ahora tomará todo el ancho de la tarjeta
                                height: 'auto',   // Mantiene la proporción sin aplastarse
                                display: 'block'
                            }} 
                        />
                    </Box>

                    {/* Aumentamos de variant="h5" a variant="h4" para hacerlo más grande */}
                    <Typography component="h1" variant="h4" align="center" gutterBottom fontWeight="bold">
                        Sistema de Tickets
                    </Typography>
                    
                    {/* Aumentamos de variant="body2" a variant="subtitle1" (o "body1") */}
                    <Typography variant="subtitle1" align="center" color="textSecondary" sx={{ mb: 4 }}>
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