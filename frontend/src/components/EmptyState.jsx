import { Box, Typography, Button } from '@mui/material';
import InboxIcon from '@mui/icons-material/Inbox';

/**
 * Estado vacío reutilizable.
 *
 * Una tabla sin datos debe explicar por qué está vacía y ofrecer la acción
 * que la llena. Antes las pantallas mostraban, como mucho, un "No hay datos"
 * suelto sin contexto ni salida.
 *
 * @param {React.ElementType} icono   Icono de MUI que ilustra el vacío.
 * @param {string} titulo             Qué falta, en una línea.
 * @param {string} descripcion        Por qué está vacío o qué hacer.
 * @param {string} textoAccion        Etiqueta del botón (opcional).
 * @param {Function} onAccion         Handler del botón (opcional).
 */
export default function EmptyState({
    icono: Icono = InboxIcon,
    titulo = 'No hay datos',
    descripcion,
    textoAccion,
    onAccion,
    sx = {},
}) {
    return (
        <Box
            sx={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                textAlign: 'center',
                py: 8,
                px: 3,
                ...sx,
            }}
        >
            <Icono
                // Decorativo: el texto que sigue ya comunica el estado, así que
                // el lector de pantalla no debe anunciar el icono.
                aria-hidden="true"
                sx={{ fontSize: 64, color: 'grey.300', mb: 2 }}
            />

            <Typography variant="h6" color="text.primary" gutterBottom>
                {titulo}
            </Typography>

            {descripcion && (
                <Typography
                    variant="body2"
                    color="text.secondary"
                    sx={{ maxWidth: 420, mb: textoAccion ? 3 : 0 }}
                >
                    {descripcion}
                </Typography>
            )}

            {textoAccion && onAccion && (
                <Button variant="contained" onClick={onAccion}>
                    {textoAccion}
                </Button>
            )}
        </Box>
    );
}
