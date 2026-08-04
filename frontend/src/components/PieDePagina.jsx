import { Box, Typography } from '@mui/material';

/**
 * Pie de página del sistema.
 *
 * El año se calcula en cada render en lugar de escribirse en el código: un
 * literal obliga a acordarse de cambiarlo cada 1 de enero, y en la práctica
 * nadie lo hace —el pie se queda anunciando un año que ya pasó.
 *
 * Vive dentro de `MainLayout`, así que aparece en todas las pantallas sin que
 * cada una tenga que montarlo.
 */
export default function PieDePagina() {
    const anio = new Date().getFullYear();

    return (
        <Box
            component="footer"
            // `mt: auto` lo empuja abajo cuando el contenido es corto, sin
            // recurrir a posición fija: así nunca tapa la última fila de una
            // tabla ni se superpone al contenido al hacer scroll.
            sx={{
                mt: 'auto',
                pt: 3,
                pb: 1,
                textAlign: 'center',
            }}
        >
            <Typography variant="caption" color="text.secondary">
                Departamento de Soporte Técnico {anio}
            </Typography>
        </Box>
    );
}
