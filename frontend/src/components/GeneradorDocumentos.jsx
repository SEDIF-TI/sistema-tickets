import React, { useState, useContext } from 'react';
import { 
    Box, Paper, Typography, TextField, Button, 
    Tabs, Tab, Grid, IconButton, Divider, CircularProgress 
} from '@mui/material';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import AddCircleIcon from '@mui/icons-material/AddCircle';
import DeleteIcon from '@mui/icons-material/Delete';
import InventoryIcon from '@mui/icons-material/Inventory';
import EmailIcon from '@mui/icons-material/Email';
import { AuthContext } from '../context/AuthContext';
import api from '../services/api'; 

export default function GeneradorDocumentos() {
    const { user } = useContext(AuthContext);
    const [cargando, setCargando] = useState(false);

    // ==========================================
    // ESTADO PARA LAS PESTAÑAS (0 = Memo, 1 = Requisición)
    // ==========================================
    const [tabIndex, setTabIndex] = useState(0);
    const handleTabChange = (event, newValue) => {
        setTabIndex(newValue);
    };

    // ==========================================
    // FUNCIÓN MAESTRA PARA DESCARGAR EL PDF
    // ==========================================
    const procesarDescargaPDF = (data, nombreArchivo) => {
        // 1. Creamos un objeto Blob con los datos binarios del backend
        const blob = new Blob([data], { type: 'application/pdf' });
        // 2. Creamos una URL temporal para ese archivo
        const url = window.URL.createObjectURL(blob);
        // 3. Creamos un enlace <a> invisible
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', nombreArchivo);
        // 4. Simulamos que el usuario le da clic al enlace para descargar
        document.body.appendChild(link);
        link.click();
        // 5. Limpiamos la basura
        link.parentNode.removeChild(link);
        window.URL.revokeObjectURL(url);
    };

    // ==========================================
    // ESTADOS Y LÓGICA: MEMORÁNDUM
    // ==========================================
    const [memoDestinatario, setMemoDestinatario] = useState('');
    const [memoRemitente, setMemoRemitente] = useState('Área de Soporte Técnico');
    const [memoAsunto, setMemoAsunto] = useState('');
    const [memoCuerpo, setMemoCuerpo] = useState('');

    const handleGenerarMemo = async (e) => {
        e.preventDefault();
        setCargando(true);
        try {
            // Reemplaza '/v1/documentos/memorandum' con la ruta real de tu controlador Spring Boot
            const response = await api.post('/v1/documentos/memorandum', {
                destinatario: memoDestinatario,
                remitente: memoRemitente,
                asunto: memoAsunto,
                cuerpo: memoCuerpo
            }, { 
                responseType: 'blob' // ¡CRÍTICO! Le dice a Axios que espere un archivo, no un JSON
            });

            procesarDescargaPDF(response.data, `Memorandum_${new Date().getTime()}.pdf`);
            
        } catch (error) {
            console.error("Error al generar PDF de Memorándum:", error);
            alert("Error al contactar con el servidor para generar el PDF.");
        } finally {
            setCargando(false);
        }
    };

    // ==========================================
    // ESTADOS Y LÓGICA: REQUISICIÓN DE MATERIALES
    // ==========================================
    const [reqSolicitante, setReqSolicitante] = useState(user?.nombre || '');
    const [reqJustificacion, setReqJustificacion] = useState('');
    const [articulos, setArticulos] = useState([{ cantidad: 1, unidad: 'Pieza', descripcion: '' }]);

    const agregarArticulo = () => setArticulos([...articulos, { cantidad: 1, unidad: 'Pieza', descripcion: '' }]);
    const eliminarArticulo = (index) => setArticulos(articulos.filter((_, i) => i !== index));
    const actualizarArticulo = (index, campo, valor) => {
        const nuevosArticulos = [...articulos];
        nuevosArticulos[index][campo] = valor;
        setArticulos(nuevosArticulos);
    };

    const handleGenerarRequisicion = async (e) => {
        e.preventDefault();
        setCargando(true);
        try {
            // Reemplaza '/v1/documentos/requisicion' con la ruta real de tu controlador Spring Boot
            const response = await api.post('/v1/documentos/requisicion', {
                solicitante: reqSolicitante,
                justificacion: reqJustificacion,
                articulos: articulos
            }, { 
                responseType: 'blob' // ¡CRÍTICO! 
            });

            procesarDescargaPDF(response.data, `Requisicion_${new Date().getTime()}.pdf`);
            
        } catch (error) {
            console.error("Error al generar PDF de Requisición:", error);
            alert("Error al contactar con el servidor para generar el PDF.");
        } finally {
            setCargando(false);
        }
    };

    return (
        <Box sx={{ width: '100%', maxWidth: 900, margin: '0 auto' }}>
            <Typography variant="h4" fontWeight="bold" color="primary" sx={{ mb: 3 }}>
                Generador de Documentos Oficiales
            </Typography>

            <Paper elevation={3} sx={{ mb: 3, borderRadius: 2 }}>
                <Tabs value={tabIndex} onChange={handleTabChange} indicatorColor="primary" textColor="primary" variant="fullWidth">
                    <Tab icon={<EmailIcon />} label="Memorándum" iconPosition="start" sx={{ fontWeight: 'bold' }} />
                    <Tab icon={<InventoryIcon />} label="Requisición de Material" iconPosition="start" sx={{ fontWeight: 'bold' }} />
                </Tabs>
            </Paper>

            {/* VISTA 1: MEMORÁNDUM */}
            {tabIndex === 0 && (
                <Paper elevation={3} sx={{ p: 4, borderRadius: 2 }}>
                    <form onSubmit={handleGenerarMemo}>
                        <Grid container spacing={3}>
                            <Grid item xs={12} sm={6}>
                                <TextField fullWidth label="Para (Destinatario) *" required value={memoDestinatario} onChange={(e) => setMemoDestinatario(e.target.value)} disabled={cargando} />
                            </Grid>
                            <Grid item xs={12} sm={6}>
                                <TextField fullWidth label="De (Remitente) *" required value={memoRemitente} onChange={(e) => setMemoRemitente(e.target.value)} disabled={cargando} />
                            </Grid>
                            <Grid item xs={12}>
                                <TextField fullWidth label="Asunto *" required value={memoAsunto} onChange={(e) => setMemoAsunto(e.target.value)} disabled={cargando} />
                            </Grid>
                            <Grid item xs={12}>
                                <TextField fullWidth label="Cuerpo del Mensaje *" required multiline rows={8} value={memoCuerpo} onChange={(e) => setMemoCuerpo(e.target.value)} disabled={cargando} />
                            </Grid>
                            <Grid item xs={12} sx={{ display: 'flex', justifyContent: 'flex-end' }}>
                                <Button type="submit" variant="contained" disabled={cargando} sx={{ bgcolor: '#5c0a28', '&:hover': { bgcolor: '#42071c' } }} startIcon={cargando ? <CircularProgress size={20} color="inherit" /> : <PictureAsPdfIcon />}>
                                    {cargando ? 'Generando...' : 'Generar PDF'}
                                </Button>
                            </Grid>
                        </Grid>
                    </form>
                </Paper>
            )}

            {/* VISTA 2: REQUISICIÓN DE MATERIALES */}
            {tabIndex === 1 && (
                <Paper elevation={3} sx={{ p: 4, borderRadius: 2 }}>
                    <form onSubmit={handleGenerarRequisicion}>
                        <Grid container spacing={3}>
                            <Grid item xs={12} sm={6}>
                                <TextField fullWidth label="Área Solicitante" disabled value="Soporte Técnico" />
                            </Grid>
                            <Grid item xs={12} sm={6}>
                                <TextField fullWidth label="Nombre del Solicitante *" required value={reqSolicitante} onChange={(e) => setReqSolicitante(e.target.value)} disabled={cargando} />
                            </Grid>
                            <Grid item xs={12}>
                                <TextField fullWidth label="Justificación de la Compra/Petición *" required value={reqJustificacion} onChange={(e) => setReqJustificacion(e.target.value)} disabled={cargando} />
                            </Grid>
                            <Grid item xs={12}>
                                <Divider sx={{ my: 1 }} />
                                <Typography variant="h6" fontWeight="bold" color="primary" sx={{ mb: 2, mt: 1 }}>Lista de Artículos</Typography>
                            </Grid>
                            {articulos.map((articulo, index) => (
                                <Grid item xs={12} key={index}>
                                    <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
                                        <TextField label="Cant." type="number" required sx={{ width: '100px' }} inputProps={{ min: 1 }} value={articulo.cantidad} onChange={(e) => actualizarArticulo(index, 'cantidad', e.target.value)} disabled={cargando} />
                                        <TextField label="Unidad" required placeholder="Pieza, Metro, Caja..." sx={{ width: '150px' }} value={articulo.unidad} onChange={(e) => actualizarArticulo(index, 'unidad', e.target.value)} disabled={cargando} />
                                        <TextField label="Descripción del Artículo" required fullWidth value={articulo.descripcion} onChange={(e) => actualizarArticulo(index, 'descripcion', e.target.value)} disabled={cargando} />
                                        <IconButton color="error" onClick={() => eliminarArticulo(index)} disabled={articulos.length === 1 || cargando}><DeleteIcon /></IconButton>
                                    </Box>
                                </Grid>
                            ))}
                            <Grid item xs={12}>
                                <Button variant="outlined" startIcon={<AddCircleIcon />} onClick={agregarArticulo} disabled={cargando} sx={{ mt: 1 }}>Agregar Artículo</Button>
                            </Grid>
                            <Grid item xs={12} sx={{ display: 'flex', justifyContent: 'flex-end', mt: 2 }}>
                                <Button type="submit" variant="contained" disabled={cargando} sx={{ bgcolor: '#5c0a28', '&:hover': { bgcolor: '#42071c' } }} startIcon={cargando ? <CircularProgress size={20} color="inherit" /> : <PictureAsPdfIcon />}>
                                    {cargando ? 'Generando...' : 'Generar Requisición (PDF)'}
                                </Button>
                            </Grid>
                        </Grid>
                    </form>
                </Paper>
            )}
        </Box>
    );
}