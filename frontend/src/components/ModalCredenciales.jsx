import { useRef } from 'react';
import {
    Dialog, DialogTitle, DialogContent, DialogActions, Button, Typography,
    Box, Paper, Divider, Stack, Alert, Tooltip, useMediaQuery
} from '@mui/material';
import { useTheme } from '@mui/material/styles';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutlined';
import PrintIcon from '@mui/icons-material/Print';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';

import { useNotification } from '../context/NotificationContext.jsx';

/**
 * Credenciales recién generadas de un usuario.
 *
 * La contraseña temporal solo viaja en la respuesta que la crea: si no se
 * copia o se imprime ahora, se pierde y hay que restablecerla.
 *
 * Cambios respecto a la versión anterior:
 *  - La impresión construía una plantilla HTML concatenando los datos del
 *    usuario y la volcaba con `document.write()`. Un nombre o un área que
 *    contuvieran `<script>` —o cualquier etiqueta— se ejecutaban en la ventana
 *    nueva. Ahora se imprime el nodo ya renderizado por React, que escapa el
 *    texto por construcción, y no se genera HTML por concatenación.
 *  - Si el navegador bloqueaba la ventana emergente, `printWindow` era `null` y
 *    la función reventaba con un error en consola sin decir nada al usuario.
 *  - No había forma de copiar la contraseña: había que teclearla a la vista.
 */
export default function ModalCredenciales({ open, onClose, usuarioData }) {
    const { notificar, notificarError } = useNotification();
    const theme = useTheme();
    const esMovil = useMediaQuery(theme.breakpoints.down('sm'));

    // Nodo que se manda a imprimir: es el mismo que ve el usuario en pantalla.
    const areaImprimible = useRef(null);

    const copiar = async () => {
        try {
            await navigator.clipboard.writeText(usuarioData?.password ?? '');
            notificar('Contraseña copiada al portapapeles.');
        } catch {
            // El portapapeles exige contexto seguro (HTTPS o localhost).
            notificarError('El navegador no permitió copiar. Selecciona el texto y cópialo a mano.');
        }
    };

    const imprimir = () => {
        const ventana = window.open('', '_blank', 'width=640,height=560');

        if (!ventana) {
            notificarError('El navegador bloqueó la ventana. Permite las ventanas emergentes para imprimir.');
            return;
        }

        // Se clona el nodo ya renderizado en lugar de rearmar la tarjeta con
        // cadenas: React escapó el contenido al pintarlo, así que el texto del
        // usuario no puede convertirse en marcado.
        const contenido = areaImprimible.current?.cloneNode(true);

        const doc = ventana.document;
        doc.title = 'Credenciales de acceso';

        const estilo = doc.createElement('style');
        estilo.textContent = `
            body { font-family: system-ui, sans-serif; color: #111; padding: 32px; }
            .tarjeta { max-width: 420px; margin: 0 auto; border: 1px dashed #999;
                       border-radius: 8px; padding: 24px; }
            .pie { margin-top: 20px; padding-top: 12px; border-top: 1px solid #ddd;
                   font-size: 11px; color: #555; }
            @media print { body { padding: 0; } }
        `;
        doc.head.appendChild(estilo);

        const tarjeta = doc.createElement('div');
        tarjeta.className = 'tarjeta';
        if (contenido) tarjeta.appendChild(contenido);

        const pie = doc.createElement('p');
        pie.className = 'pie';
        pie.textContent = 'CONFIDENCIAL: entregue este documento a la persona interesada. '
            + 'El sistema le exigirá cambiar esta contraseña la primera vez que entre.';
        tarjeta.appendChild(pie);

        doc.body.appendChild(tarjeta);

        // Se imprime tras el repintado para que el contenido esté maquetado.
        ventana.focus();
        setTimeout(() => {
            ventana.print();
            ventana.close();
        }, 250);
    };

    return (
        <Dialog
            open={open}
            onClose={onClose}
            maxWidth="sm"
            fullWidth
            fullScreen={esMovil}
            aria-labelledby="titulo-credenciales"
        >
            <DialogTitle id="titulo-credenciales" sx={{ textAlign: 'center', pb: 1 }}>
                <CheckCircleOutlineIcon color="success" sx={{ fontSize: 48, mb: 1 }} aria-hidden="true" />
                <Typography variant="h6" component="p" sx={{ fontWeight: 600 }}>
                    Cuenta creada
                </Typography>
            </DialogTitle>

            <DialogContent dividers>
                <Alert severity="warning" sx={{ mb: 3 }}>
                    Esta contraseña se muestra una sola vez. Si cierras sin copiarla ni
                    imprimirla, tendrás que restablecerla.
                </Alert>

                <Paper ref={areaImprimible} variant="outlined" sx={{ p: 2.5 }}>
                    <Typography variant="overline" color="text.secondary">
                        Datos de acceso
                    </Typography>

                    <Stack spacing={1.5} sx={{ mt: 1.5 }}>
                        {usuarioData?.nombre && (
                            <Box sx={{ display: 'flex', justifyContent: 'space-between', gap: 2 }}>
                                <Typography variant="body2" color="text.secondary">Nombre</Typography>
                                <Typography variant="body2" fontWeight={500} sx={{ textAlign: 'right' }}>
                                    {usuarioData.nombre}
                                </Typography>
                            </Box>
                        )}

                        {usuarioData?.area && (
                            <Box sx={{ display: 'flex', justifyContent: 'space-between', gap: 2 }}>
                                <Typography variant="body2" color="text.secondary">Área</Typography>
                                <Typography variant="body2" sx={{ textAlign: 'right' }}>
                                    {usuarioData.area}
                                </Typography>
                            </Box>
                        )}

                        {usuarioData?.rol && (
                            <Box sx={{ display: 'flex', justifyContent: 'space-between', gap: 2 }}>
                                <Typography variant="body2" color="text.secondary">Rol</Typography>
                                <Typography variant="body2" sx={{ textAlign: 'right' }}>
                                    {usuarioData.rol}
                                </Typography>
                            </Box>
                        )}

                        <Divider />

                        <Box sx={{ display: 'flex', justifyContent: 'space-between', gap: 2 }}>
                            <Typography variant="body2" color="text.secondary">Usuario</Typography>
                            <Typography variant="body2" fontWeight={500} sx={{ textAlign: 'right', wordBreak: 'break-all' }}>
                                {usuarioData?.correo}
                            </Typography>
                        </Box>

                        <Box>
                            <Typography variant="body2" color="text.secondary" gutterBottom>
                                Contraseña temporal
                            </Typography>
                            <Typography
                                variant="h5"
                                component="p"
                                sx={{
                                    // Monoespaciada y con separación: se dicta y
                                    // se teclea a mano, y hay que distinguir
                                    // caracteres parecidos.
                                    fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
                                    letterSpacing: 1.5,
                                    wordBreak: 'break-all',
                                }}
                            >
                                {usuarioData?.password}
                            </Typography>
                        </Box>
                    </Stack>
                </Paper>
            </DialogContent>

            <DialogActions
                sx={{ flexDirection: { xs: 'column-reverse', sm: 'row' }, gap: 1, p: 2 }}
            >
                <Button
                    onClick={onClose}
                    color="inherit"
                    fullWidth={esMovil}
                    size={esMovil ? 'large' : 'medium'}
                >
                    Cerrar
                </Button>

                <Tooltip title="Copiar la contraseña" arrow>
                    <span>
                        <Button
                            onClick={copiar}
                            startIcon={<ContentCopyIcon />}
                            fullWidth={esMovil}
                            size={esMovil ? 'large' : 'medium'}
                        >
                            Copiar
                        </Button>
                    </span>
                </Tooltip>

                <Button
                    onClick={imprimir}
                    variant="contained"
                    startIcon={<PrintIcon />}
                    fullWidth={esMovil}
                    size={esMovil ? 'large' : 'medium'}
                >
                    Imprimir
                </Button>
            </DialogActions>
        </Dialog>
    );
}
