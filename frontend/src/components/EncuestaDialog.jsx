import { useState, useEffect } from 'react';
import {
    Dialog, DialogTitle, DialogContent, DialogActions, Button, Typography,
    Box, Stack, TextField, useMediaQuery
} from '@mui/material';
import { useTheme } from '@mui/material/styles';
import SentimentVeryDissatisfiedIcon from '@mui/icons-material/SentimentVeryDissatisfied';
import SentimentNeutralIcon from '@mui/icons-material/SentimentNeutral';
import SentimentSatisfiedAltIcon from '@mui/icons-material/SentimentSatisfiedAlt';

import { CALIFICACION } from '../util/calificacion';

/**
 * Opciones de la encuesta.
 *
 * Cada una lleva icono, texto y color propios: la identidad no puede depender
 * solo del color, porque quien no distingue el rojo del verde se quedaría sin
 * saber cuál está eligiendo.
 */
const OPCIONES = [
    {
        valor: CALIFICACION.MALO,
        etiqueta: 'Malo',
        descripcion: 'No resolvió mi problema',
        icono: SentimentVeryDissatisfiedIcon,
        color: 'error',
    },
    {
        valor: CALIFICACION.REGULAR,
        etiqueta: 'Regular',
        descripcion: 'Se resolvió, pero con reservas',
        icono: SentimentNeutralIcon,
        color: 'warning',
    },
    {
        valor: CALIFICACION.BUENO,
        etiqueta: 'Bueno',
        descripcion: 'Quedé conforme con la atención',
        icono: SentimentSatisfiedAltIcon,
        color: 'success',
    },
];

/**
 * Encuesta de satisfacción que se ofrece al cerrar un ticket.
 *
 * Tres opciones y no cinco estrellas: la escala corta se contesta de un
 * vistazo, y quien acaba de recibir una reparación responde en un segundo o no
 * responde en absoluto.
 *
 * La encuesta es voluntaria. Se puede omitir sin penalización, porque forzarla
 * solo produciría respuestas al azar que ensuciarían la métrica del técnico.
 *
 * @param {boolean}  abierto
 * @param {object}   ticket        El ticket recién cerrado.
 * @param {boolean}  enviando
 * @param {Function} onEnviar      Recibe (calificacion, comentario).
 * @param {Function} onOmitir
 */
export default function EncuestaDialog({
    abierto,
    ticket,
    enviando = false,
    onEnviar,
    onOmitir,
}) {
    const theme = useTheme();
    const esMovil = useMediaQuery(theme.breakpoints.down('sm'));

    const [elegida, setElegida] = useState(null);
    const [comentario, setComentario] = useState('');

    // Cada ticket estrena su propia encuesta: sin esto, la respuesta anterior
    // quedaría preseleccionada al calificar el siguiente.
    useEffect(() => {
        if (abierto) {
            setElegida(null);
            setComentario('');
        }
    }, [abierto, ticket?.id]);

    return (
        <Dialog
            open={abierto}
            onClose={enviando ? undefined : onOmitir}
            maxWidth="sm"
            fullWidth
            fullScreen={esMovil}
            aria-labelledby="titulo-encuesta"
        >
            <DialogTitle id="titulo-encuesta">
                ¿Cómo fue el servicio?
            </DialogTitle>

            <DialogContent dividers>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                    {ticket?.usuarioSoporteNombre
                        ? <>Tu ticket #{ticket?.id} lo atendió <strong>{ticket.usuarioSoporteNombre}</strong>. Tu respuesta nos ayuda a mejorar la atención.</>
                        : <>Cuéntanos cómo te fue con el ticket #{ticket?.id}. Tu respuesta nos ayuda a mejorar la atención.</>}
                </Typography>

                {/* radiogroup y no botones sueltos: un lector de pantalla
                    anuncia el conjunto como una sola pregunta con tres
                    respuestas, y las flechas del teclado navegan entre ellas. */}
                <Stack
                    direction={{ xs: 'column', sm: 'row' }}
                    spacing={1.5}
                    role="radiogroup"
                    aria-label="Calificación del servicio"
                >
                    {OPCIONES.map((opcion) => {
                        const Icono = opcion.icono;
                        const activa = elegida === opcion.valor;

                        return (
                            <Box
                                key={opcion.valor}
                                component="button"
                                type="button"
                                role="radio"
                                aria-checked={activa}
                                onClick={() => setElegida(opcion.valor)}
                                disabled={enviando}
                                sx={{
                                    flex: 1,
                                    display: 'flex',
                                    flexDirection: { xs: 'row', sm: 'column' },
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    gap: 1,
                                    p: 2,
                                    cursor: 'pointer',
                                    borderRadius: 2,
                                    bgcolor: 'background.paper',
                                    // El borde grueso marca la elección además
                                    // del color, para que se distinga sin él.
                                    border: '2px solid',
                                    borderColor: activa ? `${opcion.color}.main` : 'divider',
                                    color: activa ? `${opcion.color}.main` : 'text.secondary',
                                    transition: 'border-color 150ms, color 150ms',
                                    font: 'inherit',
                                    '&:hover:not(:disabled)': { borderColor: `${opcion.color}.main` },
                                    '&:disabled': { cursor: 'default', opacity: 0.6 },
                                }}
                            >
                                <Icono sx={{ fontSize: 36 }} aria-hidden="true" />
                                <Box sx={{ textAlign: { xs: 'left', sm: 'center' } }}>
                                    <Typography variant="body2" sx={{ fontWeight: activa ? 600 : 500 }}>
                                        {opcion.etiqueta}
                                    </Typography>
                                    <Typography variant="caption" color="text.secondary">
                                        {opcion.descripcion}
                                    </Typography>
                                </Box>
                            </Box>
                        );
                    })}
                </Stack>

                <TextField
                    fullWidth
                    multiline
                    rows={3}
                    label="¿Quieres contarnos algo más?"
                    placeholder="Opcional"
                    value={comentario}
                    onChange={(e) => setComentario(e.target.value)}
                    disabled={enviando}
                    slotProps={{ htmlInput: { maxLength: 500 } }}
                    helperText={`Opcional · ${comentario.length}/500`}
                    sx={{ mt: 3 }}
                />
            </DialogContent>

            <DialogActions
                sx={{ flexDirection: { xs: 'column-reverse', sm: 'row' }, gap: 1, p: 2 }}
            >
                {/* Omitir es una salida legítima, no un castigo: una encuesta
                    obligatoria solo genera respuestas al azar. */}
                <Button
                    onClick={onOmitir}
                    color="inherit"
                    disabled={enviando}
                    fullWidth={esMovil}
                    size={esMovil ? 'large' : 'medium'}
                >
                    Ahora no
                </Button>
                <Button
                    onClick={() => onEnviar(elegida, comentario.trim() || null)}
                    variant="contained"
                    disabled={!elegida || enviando}
                    fullWidth={esMovil}
                    size={esMovil ? 'large' : 'medium'}
                >
                    {enviando ? 'Enviando…' : 'Enviar respuesta'}
                </Button>
            </DialogActions>
        </Dialog>
    );
}
