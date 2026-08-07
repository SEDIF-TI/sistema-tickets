import { useState, useContext } from 'react';
import {
    Box, Typography, Paper, Button, Alert, Divider, Stack, useMediaQuery
} from '@mui/material';
import { useTheme } from '@mui/material/styles';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import LockResetIcon from '@mui/icons-material/LockReset';
import TelegramIcon from '@mui/icons-material/Telegram';
import NotificationsActiveIcon from '@mui/icons-material/NotificationsActive';
import AccountCircleIcon from '@mui/icons-material/AccountCircle';

import { AuthContext } from '../context/AuthContext.jsx';
import { perfilService } from '../services/perfilService';
import { useNotification } from '../context/NotificationContext.jsx';
import { useNetworkStatus } from '../hooks/useNetworkStatus.jsx';
import { useRol } from '../hooks/useRol.jsx';
import { esquemaPassword } from '../util/esquemas';

import CampoFormulario from '../components/CampoFormulario';

const VALORES_INICIALES = {
    passwordActual: '',
    nuevaPassword: '',
    confirmarPassword: '',
};

/**
 * Perfil del usuario: cambio de contraseña y vinculación con Telegram.
 *
 * El cambio de contraseña va contra `/v1/perfil` y exige la contraseña actual,
 * que el backend verifica antes de aceptar la nueva; queda registrado en la
 * auditoría. El esquema `esquemaPassword` valida en el navegador las mismas
 * reglas que aplica el servidor —ocho caracteres con al menos una letra y un
 * número— para no enviar una clave que se vaya a rechazar.
 *
 * Cuando el usuario entra con una contraseña temporal, el enrutador lo retiene
 * en esta pantalla: cambiarla es obligatorio. Por eso, tras guardarla con
 * éxito, se avisa al AuthContext con `marcarPasswordCambiada` y se navega a la
 * raíz, que ya reparte según el rol.
 *
 * La vinculación con Telegram solo se ofrece al personal técnico, que es quien
 * recibe avisos de asignación. Abre la conversación con el bot pasando el id
 * de usuario en el parámetro `start`: ese valor es el que el bot usa para
 * asociar el chat con la cuenta del sistema. El bot se toma del entorno
 * (`VITE_TELEGRAM_BOT_USERNAME`) para poder apuntar a otro sin recompilar.
 */
export default function PerfilPage() {
    const { user, marcarPasswordCambiada } = useContext(AuthContext);
    const { esTecnico, usuarioId, nombre } = useRol();
    const { notificar, notificarError } = useNotification();
    const navigate = useNavigate();

    const theme = useTheme();
    const esMovil = useMediaQuery(theme.breakpoints.down('sm'));

    const estadoRed = useNetworkStatus();
    const sinConexion = Boolean(estadoRed.sinConexion ?? estadoRed);

    const [vinculando, setVinculando] = useState(false);

    const claveTemporal = Boolean(user?.passwordTemporal);

    const {
        control,
        handleSubmit,
        reset,
        formState: { isSubmitting },
    } = useForm({
        resolver: zodResolver(esquemaPassword),
        mode: 'onBlur',
        defaultValues: VALORES_INICIALES,
    });

    const cambiarPassword = async (datos) => {
        try {
            const respuesta = await perfilService.cambiarPassword(
                datos.passwordActual,
                datos.nuevaPassword
            );

            notificar(respuesta?.mensaje || 'Contraseña actualizada correctamente.');
            reset(VALORES_INICIALES);

            if (claveTemporal) {
                // Con la clave ya cambiada, el usuario deja de estar retenido
                // en esta pantalla y entra al sistema.
                marcarPasswordCambiada();
                navigate('/', { replace: true });
            }
        } catch (error) {
            notificarError(error);
        }
    };

    const vincularTelegram = () => {
        const bot = import.meta.env.VITE_TELEGRAM_BOT_USERNAME;

        if (!bot) {
            notificarError('El bot de notificaciones no está configurado. Avisa al área de sistemas.');
            return;
        }

        if (!usuarioId) {
            notificarError('No se pudo identificar tu cuenta. Cierra la sesión y vuelve a entrar.');
            return;
        }

        setVinculando(true);
        // El parámetro `start` lleva el id de usuario: es lo que el bot usa
        // para asociar la conversación con la cuenta del sistema.
        const ventana = window.open(`https://t.me/${bot}?start=${usuarioId}`, '_blank');

        if (!ventana) {
            notificarError('El navegador bloqueó la ventana. Permite las ventanas emergentes para abrir Telegram.');
        } else {
            notificar('Se abrió Telegram. Pulsa "Iniciar" en la conversación para completar la vinculación.');
        }

        setVinculando(false);
    };

    return (
        <Box sx={{ maxWidth: 960, mx: 'auto' }}>
            {claveTemporal && (
                <Alert severity="warning" sx={{ mb: 3 }}>
                    Estás usando una contraseña temporal. Cámbiala para poder entrar al
                    resto del sistema.
                </Alert>
            )}

            <Box sx={{ mb: 3 }}>
                <Typography
                    variant="h4"
                    component="h2"
                    color="primary.main"
                    sx={{ display: 'flex', alignItems: 'center', gap: 1 }}
                >
                    <AccountCircleIcon fontSize="large" aria-hidden="true" />
                    Mi perfil
                </Typography>
                <Typography variant="body2" color="text.secondary">
                    {nombre ? `Sesión de ${nombre}.` : 'Configuración de tu cuenta.'}
                </Typography>
            </Box>

            <Box
                sx={{
                    display: 'grid',
                    gridTemplateColumns: { xs: '1fr', md: esTecnico ? 'repeat(2, 1fr)' : '1fr' },
                    gap: 3,
                    alignItems: 'start',
                }}
            >
                <Paper variant="outlined" sx={{ p: { xs: 2.5, sm: 3.5 } }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                        <LockResetIcon color="primary" aria-hidden="true" />
                        <Typography variant="h6" component="h3">
                            Contraseña
                        </Typography>
                    </Box>
                    <Typography variant="body2" color="text.secondary">
                        Debe tener al menos 8 caracteres, con una letra y un número.
                    </Typography>

                    <Divider sx={{ my: 2.5 }} />

                    <form onSubmit={handleSubmit(cambiarPassword)} noValidate>
                        <Stack spacing={2.5}>
                            <CampoFormulario
                                control={control}
                                nombre="passwordActual"
                                etiqueta="Contraseña actual"
                                obligatorio
                                type="password"
                                autoComplete="current-password"
                                ayuda={claveTemporal
                                    ? 'La contraseña temporal que te entregaron.'
                                    : undefined}
                            />

                            <CampoFormulario
                                control={control}
                                nombre="nuevaPassword"
                                etiqueta="Nueva contraseña"
                                obligatorio
                                type="password"
                                autoComplete="new-password"
                            />

                            <CampoFormulario
                                control={control}
                                nombre="confirmarPassword"
                                etiqueta="Repite la nueva contraseña"
                                obligatorio
                                type="password"
                                autoComplete="new-password"
                            />

                            <Button
                                type="submit"
                                variant="contained"
                                size="large"
                                disabled={isSubmitting || sinConexion}
                                fullWidth
                            >
                                {isSubmitting
                                    ? 'Guardando…'
                                    : claveTemporal ? 'Cambiar y entrar al sistema' : 'Cambiar contraseña'}
                            </Button>
                        </Stack>
                    </form>
                </Paper>

                {esTecnico && (
                    <Paper variant="outlined" sx={{ p: { xs: 2.5, sm: 3.5 } }}>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                            <NotificationsActiveIcon color="primary" aria-hidden="true" />
                            <Typography variant="h6" component="h3">
                                Notificaciones
                            </Typography>
                        </Box>
                        <Typography variant="body2" color="text.secondary">
                            Recibe un aviso en tu teléfono cuando se te asigne un ticket.
                        </Typography>

                        <Divider sx={{ my: 2.5 }} />

                        {claveTemporal ? (
                            <Alert severity="info">
                                Cambia primero tu contraseña temporal; después podrás vincular
                                Telegram.
                            </Alert>
                        ) : (
                            <Stack spacing={2.5}>
                                <Typography variant="body2" color="text.secondary">
                                    Al pulsar el botón se abrirá una conversación con el bot del
                                    sistema. Pulsa <strong>Iniciar</strong> dentro de Telegram y
                                    tu cuenta quedará vinculada.
                                </Typography>

                                <Button
                                    onClick={vincularTelegram}
                                    variant="contained"
                                    size={esMovil ? 'large' : 'medium'}
                                    startIcon={<TelegramIcon />}
                                    disabled={vinculando || sinConexion}
                                    fullWidth
                                >
                                    Vincular con Telegram
                                </Button>
                            </Stack>
                        )}
                    </Paper>
                )}
            </Box>
        </Box>
    );
}
