import { Paper, Box, Typography } from '@mui/material';

/**
 * Cifra destacada del panel.
 *
 * Cuando el dato es un solo número, un número dice más que una gráfica: una
 * barra única o un pastel de dos porciones ocupan diez veces más espacio para
 * comunicar lo mismo.
 *
 * La cifra se deja en tipografía proporcional y no en `tabular-nums`: a este
 * tamaño los dígitos de ancho fijo separan visualmente un "121". El ancho fijo
 * se reserva para las tablas, donde sí hay cifras que alinear.
 *
 * @param {string} etiqueta  Qué se está contando.
 * @param {number|string} valor  La cifra.
 * @param {React.ElementType} icono  Icono ilustrativo, decorativo.
 * @param {string} detalle  Contexto breve bajo la cifra.
 * @param {string} color  Color del icono; por defecto, el de la marca.
 */
export default function TarjetaMetrica({
    etiqueta,
    valor,
    icono: Icono,
    detalle,
    color = 'primary.main',
}) {
    return (
        <Paper
            variant="outlined"
            sx={{
                p: 2.5,
                height: '100%',
                display: 'flex',
                alignItems: 'center',
                gap: 2,
            }}
        >
            {Icono && (
                <Box
                    aria-hidden="true"
                    sx={{
                        display: 'flex',
                        p: 1.25,
                        borderRadius: 2,
                        bgcolor: 'action.hover',
                        color,
                    }}
                >
                    <Icono />
                </Box>
            )}

            <Box sx={{ minWidth: 0 }}>
                <Typography variant="body2" color="text.secondary" noWrap>
                    {etiqueta}
                </Typography>
                <Typography variant="h4" component="p" sx={{ fontWeight: 600, lineHeight: 1.2 }}>
                    {valor}
                </Typography>
                {detalle && (
                    <Typography variant="caption" color="text.secondary">
                        {detalle}
                    </Typography>
                )}
            </Box>
        </Paper>
    );
}
