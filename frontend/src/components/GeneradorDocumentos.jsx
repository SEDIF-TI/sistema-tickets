import { useState } from 'react';
import { 
    Box, Typography, Paper, TextField, Button, 
    CircularProgress, Divider 
} from '@mui/material';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import api from '../services/api'; // Ajusta la ruta de tu instancia de Axios

export default function GeneradorDocumentos() {
    const [cargando, setCargando] = useState(false);
    const [formulario, setFormulario] = useState({
        destinatario: '',
        remitente: 'Área de Soporte Técnico', // Valor por defecto
        asunto: '',
        cuerpoMensaje: ''
    });

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormulario(prev => ({ ...prev, [name]: value }));
    };

    const generarPDF = async (e) => {
        e.preventDefault(); // Evitamos que la página se recargue
        setCargando(true);

        try {
            // 1. Petición POST al endpoint de documentos
            // CRÍTICO: responseType: 'blob' le dice a Axios que no espere un JSON, sino un archivo binario
            const respuesta = await api.post('/v1/documentos/memorandum', formulario, {
                responseType: 'blob' 
            });

            // 2. Crear una URL temporal en la memoria del navegador con el archivo recibido
            const blob = new Blob([respuesta.data], { type: 'application/pdf' });
            const url = window.URL.createObjectURL(blob);

            // 3. Crear un enlace invisible y forzar el clic para descargar
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `Memorandum_${new Date().getTime()}.pdf`); // Nombre dinámico
            document.body.appendChild(link);
            link.click();

            // 4. Limpieza de memoria
            link.parentNode.removeChild(link);
            window.URL.revokeObjectURL(url);

        } catch (error) {
            console.error("Error al generar el documento:", error);
            alert("Hubo un problema al generar el PDF. Revisa tu conexión o sesión.");
        } finally {
            setCargando(false);
        }
    };

    return (
        <Box sx={{ width: '100%', maxWidth: 800, mx: 'auto', mt: 4 }}>
            <Paper elevation={3} sx={{ p: 4, borderRadius: 2 }}>
                
                {/* ENCABEZADO */}
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                    <PictureAsPdfIcon color="primary" sx={{ fontSize: 40, mr: 2 }} />
                    <Box>
                        <Typography variant="h5" fontWeight="bold" color="primary">
                            Generador de Memorándums
                        </Typography>
                        <Typography variant="body2" color="textSecondary">
                            Llena los datos para generar el documento oficial en formato PDF.
                        </Typography>
                    </Box>
                </Box>
                
                <Divider sx={{ mb: 4 }} />

                {/* FORMULARIO */}
                <form onSubmit={generarPDF}>
                    <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
                        <TextField
                            fullWidth
                            label="Para (Destinatario) *"
                            name="destinatario"
                            value={formulario.destinatario}
                            onChange={handleChange}
                            required
                            variant="outlined"
                        />
                        <TextField
                            fullWidth
                            label="De (Remitente) *"
                            name="remitente"
                            value={formulario.remitente}
                            onChange={handleChange}
                            required
                            variant="outlined"
                        />
                    </Box>

                    <TextField
                        fullWidth
                        label="Asunto *"
                        name="asunto"
                        value={formulario.asunto}
                        onChange={handleChange}
                        required
                        variant="outlined"
                        sx={{ mb: 3 }}
                    />

                    <Typography variant="subtitle2" sx={{ fontWeight: 'bold', mb: 1 }}>
                        Cuerpo del Mensaje *
                    </Typography>
                    <TextField
                        fullWidth
                        name="cuerpoMensaje"
                        value={formulario.cuerpoMensaje}
                        onChange={handleChange}
                        required
                        multiline
                        rows={8}
                        variant="outlined"
                        placeholder="Redacta aquí el contenido del memorándum..."
                        sx={{ mb: 4 }}
                    />

                    <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
                        <Button 
                            type="submit" 
                            variant="contained" 
                            color="primary"
                            disabled={cargando}
                            startIcon={cargando ? <CircularProgress size={20} color="inherit" /> : <PictureAsPdfIcon />}
                            sx={{ px: 4, py: 1.5, textTransform: 'none', fontSize: '1rem' }}
                        >
                            {cargando ? 'Generando...' : 'Generar PDF'}
                        </Button>
                    </Box>
                </form>

            </Paper>
        </Box>
    );
}