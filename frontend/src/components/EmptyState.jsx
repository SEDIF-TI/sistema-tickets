import { Box, Typography, Button } from '@mui/material';
import InboxIcon from '@mui/icons-material/Inbox';

/**
 * Estado vacío reutilizable, que usan tanto `DynamicTable` como las tarjetas de
 * gráfica.
 *
 * Un listado sin datos explica por qué está vacío y, cuando existe, ofrece la
 * acción que lo llena: un "No hay datos" suelto deja al usuario sin saber si
 * falta cargar algo, si el filtro es demasiado estrecho o si debe crear el
 * primer registro.
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
                // Icono decorativo: el texto que sigue ya comunica el
                // estado, así que anunciarlo solo añadiría ruido.
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
