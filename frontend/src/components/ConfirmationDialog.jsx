import {
    Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions,
    Button, Box
} from '@mui/material';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';

/**
 * Diálogo de confirmación para acciones irreversibles.
 *
 * Es la alternativa a `window.confirm()`, que bloquea el hilo del navegador, no
 * admite estilos y no respeta el idioma de la aplicación.
 *
 * La acción destructiva va a la derecha y en rojo; cancelar queda a la
 * izquierda y con menos peso visual. Mientras `cargando` está activo el diálogo
 * no se puede cerrar: una operación a medias no debe perder su confirmación.
 *
 * @param {boolean}  abierto        Controla la visibilidad.
 * @param {string}   titulo         Pregunta en una línea.
 * @param {string}   mensaje        Consecuencia de confirmar.
 * @param {string}   textoConfirmar Etiqueta del botón principal.
 * @param {string}   textoCancelar  Etiqueta del botón secundario.
 * @param {boolean}  destructivo    Pinta la confirmación en rojo.
 * @param {boolean}  cargando       Deshabilita y muestra progreso.
 * @param {Function} onConfirmar    Handler de la acción.
 * @param {Function} onCancelar     Handler de cierre.
 */
export default function ConfirmationDialog({
    abierto,
    titulo = '¿Confirmar acción?',
    mensaje,
    textoConfirmar = 'Confirmar',
    textoCancelar = 'Cancelar',
    destructivo = false,
    cargando = false,
    onConfirmar,
    onCancelar,
}) {
    return (
        <Dialog
            open={abierto}
            onClose={cargando ? undefined : onCancelar}   // no se cierra a media operación
            maxWidth="xs"
            fullWidth
            aria-labelledby="titulo-confirmacion"
            aria-describedby="mensaje-confirmacion"
        >
            <DialogTitle id="titulo-confirmacion">
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                    {destructivo && (
                        <WarningAmberIcon color="error" aria-hidden="true" />
                    )}
                    {titulo}
                </Box>
            </DialogTitle>

            <DialogContent>
                <DialogContentText id="mensaje-confirmacion">
                    {mensaje}
                </DialogContentText>
            </DialogContent>

            <DialogActions>
                {/* Cancelar primero en el orden de tabulación: si alguien
                    confirma sin querer, el camino fácil es la salida segura. */}
                <Button onClick={onCancelar} color="inherit" disabled={cargando}>
                    {textoCancelar}
                </Button>
                <Button
                    onClick={onConfirmar}
                    variant="contained"
                    color={destructivo ? 'error' : 'primary'}
                    disabled={cargando}
                    autoFocus
                >
                    {cargando ? 'Procesando…' : textoConfirmar}
                </Button>
            </DialogActions>
        </Dialog>
    );
}
