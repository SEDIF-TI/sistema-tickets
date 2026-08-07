import { useState, useCallback } from 'react';
import {
    Box, Typography, Paper, Tabs, Tab, Dialog, DialogTitle, DialogContent,
    DialogActions, IconButton, Button, TextField, MenuItem, useMediaQuery,
    CircularProgress
} from '@mui/material';
import { useTheme } from '@mui/material/styles';
import CloseIcon from '@mui/icons-material/Close';
import DescriptionIcon from '@mui/icons-material/Description';
import DownloadIcon from '@mui/icons-material/Download';

import api from '../services/api';
import { useNotification } from '../context/NotificationContext.jsx';

import DictamenFormato from './formato/DictamenFormato.jsx';
import FormularioResguardo from './formato/FormularioResguardo.jsx';
import ReporteActividadesFormato from './formato/ReporteActividadesFormato.jsx';

const FORMATOS_TECNICOS = [
    { valor: 'DICTAMEN', etiqueta: 'Dictamen técnico' },
    { valor: 'RESGUARDO', etiqueta: 'Responsiva de resguardo' },
];

/**
 * Generador de los documentos oficiales del área.
 *
 * Reúne los formatos en pestañas y les presta el mecanismo común de generación:
 * cada formato mantiene su propio estado y, al pulsar su botón, llama a
 * `solicitarPdf(endpoint, carga, nombreArchivo)`. Este componente no conoce el
 * contenido de esa carga —cada documento tiene los campos que exige su DTO—,
 * solo la envía por POST a `/v1/documentos/<endpoint>` y trata la respuesta.
 *
 * El backend responde con el PDF ya compuesto, que se pide como blob para que
 * el interceptor de api.js lo deje pasar sin desempaquetar. De ese blob se
 * deriva una URL de objeto que alimenta el visor y el botón de descarga.
 *
 * Esas URL se liberan de forma explícita —al sustituir un documento por otro y
 * al cerrar el visor—, porque el navegador retiene el blob mientras exista la
 * referencia, aunque nadie la use.
 */
export default function GeneradorDocumentos() {
    const { notificarError } = useNotification();

    const theme = useTheme();
    const esMovil = useMediaQuery(theme.breakpoints.down('md'));

    const [pestana, setPestana] = useState(0);
    const [formatoTecnico, setFormatoTecnico] = useState('DICTAMEN');

    const [pdfUrl, setPdfUrl] = useState('');
    const [nombrePdf, setNombrePdf] = useState('');
    const [generando, setGenerando] = useState(false);

    const solicitarPdf = useCallback(async (endpoint, carga, nombreArchivo) => {
        // Unos formatos envían la ruta completa y otros solo el nombre del
        // documento; se recorta el prefijo para no componer una URL con
        // /v1/documentos duplicado.
        const limpio = String(endpoint)
            .replace('/v1/documentos/', '')
            .replace(/^\/+/, '');

        setGenerando(true);
        try {
            const respuesta = await api.post(`/v1/documentos/${limpio}`, carga, {
                responseType: 'blob',
            });

            // Se libera el documento anterior antes de sustituirlo: cada URL de
            // objeto retiene su blob en memoria hasta que se revoca.
            setPdfUrl((anterior) => {
                if (anterior) window.URL.revokeObjectURL(anterior);
                return window.URL.createObjectURL(
                    new Blob([respuesta.data], { type: 'application/pdf' })
                );
            });
            setNombrePdf(nombreArchivo || 'Documento');
        } catch (error) {
            notificarError(error);
        } finally {
            setGenerando(false);
        }
    }, [notificarError]);

    const cerrarVisor = () => {
        if (pdfUrl) window.URL.revokeObjectURL(pdfUrl);
        setPdfUrl('');
    };

    const descargar = () => {
        const enlace = document.createElement('a');
        enlace.href = pdfUrl;
        enlace.download = `${nombrePdf}.pdf`;
        enlace.click();
    };

    return (
        <Box>
            <Box sx={{ mb: 3 }}>
                <Typography
                    variant="h4"
                    component="h2"
                    color="primary.main"
                    sx={{ display: 'flex', alignItems: 'center', gap: 1 }}
                >
                    <DescriptionIcon fontSize="large" aria-hidden="true" />
                    Documentos oficiales
                </Typography>
                <Typography variant="body2" color="text.secondary">
                    Dictámenes, responsivas y reportes listos para firma.
                </Typography>
            </Box>

            <Paper variant="outlined" sx={{ mb: 3 }}>
                <Tabs
                    value={pestana}
                    onChange={(_e, valor) => setPestana(valor)}
                    variant="scrollable"
                    scrollButtons="auto"
                    allowScrollButtonsMobile
                >
                    <Tab label="Dictámenes y resguardos" />
                    <Tab label="Reporte de actividades" />
                </Tabs>
            </Paper>

            <Box>
                {pestana === 0 && (
                    <Box>
                        <Paper variant="outlined" sx={{ p: 2.5, mb: 3 }}>
                            <TextField
                                select
                                fullWidth
                                label="Formato a generar"
                                value={formatoTecnico}
                                onChange={(e) => setFormatoTecnico(e.target.value)}
                                sx={{ maxWidth: { sm: 420 } }}
                            >
                                {FORMATOS_TECNICOS.map((f) => (
                                    <MenuItem key={f.valor} value={f.valor}>
                                        {f.etiqueta}
                                    </MenuItem>
                                ))}
                            </TextField>
                        </Paper>

                        {formatoTecnico === 'DICTAMEN' && (
                            <DictamenFormato solicitarPdf={solicitarPdf} generando={generando} />
                        )}
                        {formatoTecnico === 'RESGUARDO' && (
                            <FormularioResguardo solicitarPdf={solicitarPdf} generando={generando} />
                        )}
                    </Box>
                )}

                {pestana === 1 && (
                    <ReporteActividadesFormato solicitarPdf={solicitarPdf} generando={generando} />
                )}
            </Box>

            {/* Visor del PDF: se abre solo cuando hay documento generado. */}
            <Dialog
                open={Boolean(pdfUrl)}
                onClose={cerrarVisor}
                maxWidth="lg"
                fullWidth
                fullScreen={esMovil}
                aria-labelledby="titulo-visor-pdf"
            >
                <DialogTitle
                    id="titulo-visor-pdf"
                    sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 2 }}
                >
                    <Typography variant="h6" component="span" noWrap>
                        {nombrePdf}
                    </Typography>
                    <IconButton onClick={cerrarVisor} aria-label="Cerrar la vista previa">
                        <CloseIcon />
                    </IconButton>
                </DialogTitle>

                <DialogContent dividers sx={{ height: { xs: 'auto', md: '78vh' }, p: 0 }}>
                    <iframe
                        src={pdfUrl}
                        title={`Vista previa de ${nombrePdf}`}
                        style={{ border: 'none', display: 'block', width: '100%', height: '100%', minHeight: '60vh' }}
                    />
                </DialogContent>

                <DialogActions sx={{ p: 2, gap: 1 }}>
                    <Button onClick={cerrarVisor} color="inherit">
                        Cerrar
                    </Button>
                    {/* En móvil el visor embebido suele no funcionar; descargar
                        es el camino fiable. */}
                    <Button onClick={descargar} variant="contained" startIcon={<DownloadIcon />}>
                        Descargar
                    </Button>
                </DialogActions>
            </Dialog>

            {generando && (
                <Box
                    sx={{
                        position: 'fixed',
                        inset: 0,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        bgcolor: 'rgba(0,0,0,0.35)',
                        zIndex: (t) => t.zIndex.modal + 1,
                    }}
                    role="status"
                    aria-label="Generando el documento"
                >
                    <Paper sx={{ p: 3, display: 'flex', alignItems: 'center', gap: 2 }}>
                        <CircularProgress size={28} />
                        <Typography>Generando el documento…</Typography>
                    </Paper>
                </Box>
            )}
        </Box>
    );
}
