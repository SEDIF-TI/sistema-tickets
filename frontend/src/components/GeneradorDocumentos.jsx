import React, { useState } from 'react';
import { 
    Box, Typography, Paper, Tabs, Tab, FormControl, InputLabel, Select, MenuItem, Dialog, 
    DialogTitle, DialogContent, Button, IconButton 
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import api from "../services/api";

// Importaciones
import DictamenFormato from "./formato/DictamenFormato.jsx";
import FormularioResguardo from "./formato/FormularioResguardo.jsx";
import ReporteActividadesFormato from "./formato/ReporteActividadesFormato.jsx";
import MantenimientoPreventivoFormato from "./formato/MantenimientoPreventivoFormato.jsx";

const COLOR_GUINDA = '#801A36';

export default function GeneradorDocumentos({ user }) {
    const [tabIndex, setTabIndex] = useState(0);
    const [subFormatoTecnico, setSubFormatoTecnico] = useState('DICTAMEN');

    // --- ESTADOS PARA CONTROLAR EL MODAL DEL PDF ---
    const [openModal, setOpenModal] = useState(false);
    const [pdfUrl, setPdfUrl] = useState("");
    const [nombrePdfActual, setNombrePdfActual] = useState("");

    const solicitarPdf = async (endpoint, payload, nombreArchivo) => {
        try {
            // Nota: Aquí se construye la ruta final: /api/v1/documentos/{endpoint}
            // Asegúrate de enviar SOLO el nombre del endpoint desde los hijos (ej: 'dictamen')
            const response = await api.post(`/v1/documentos/${endpoint}`, payload, { responseType: 'blob' });
            
            const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
            setPdfUrl(url);
            setNombrePdfActual(nombreArchivo);
            setOpenModal(true);
        } catch (error) {
            console.error("Error al generar el PDF:", error);
            alert("Hubo un error al generar el documento. Verifica los datos.");
        }
    };

    const handleCloseModal = () => {
        if (pdfUrl) {
            window.URL.revokeObjectURL(pdfUrl); // Limpiamos la memoria
        }
        setPdfUrl("");
        setOpenModal(false);
    };

    return (
        <Box sx={{ p: 3, width: '100%', boxSizing: 'border-box' }}>
            <Typography variant="h5" fontWeight="bold" sx={{ color: COLOR_GUINDA, mb: 3 }}>
                Generador de Documentos Oficiales
            </Typography>

            {/* BARRA DE PESTAÑAS */}
            <Paper sx={{ mb: 3, borderRadius: 2, overflow: 'hidden', width: '100%' }}>
                <Tabs 
                    value={tabIndex} 
                    onChange={(e, newValue) => setTabIndex(newValue)} 
                    variant="scrollable"
                    scrollButtons="auto"
                    TabIndicatorProps={{ style: { backgroundColor: COLOR_GUINDA } }}
                    sx={{ '& .Mui-selected': { color: `${COLOR_GUINDA} !important`, fontWeight: 'bold' } }}
                >
                    <Tab label="Dictámenes y Resguardos" />
                    <Tab label="Reporte de Actividades" />
                    <Tab label="Mantenimiento Preventivo" />
                </Tabs>
            </Paper>

            <Box sx={{ width: '100%' }}>
                {/* PESTAÑA 0: Dictámenes y Resguardos */}
                {tabIndex === 0 && (
                    <Box>
                        <Paper sx={{ p: 3, mb: 3, backgroundColor: '#f9f9f9', borderRadius: 2 }} elevation={0} variant="outlined">
                            <FormControl fullWidth>
                                <InputLabel id="select-subformato-label">Formato a Generar</InputLabel>
                                <Select
                                    labelId="select-subformato-label"
                                    value={subFormatoTecnico}
                                    label="Formato a Generar"
                                    onChange={(e) => setSubFormatoTecnico(e.target.value)}
                                    sx={{ backgroundColor: 'white' }}
                                >
                                    <MenuItem value="DICTAMEN">Dictamen Técnico</MenuItem>
                                    <MenuItem value="RESGUARDO">Responsiva de Resguardo</MenuItem>
                                </Select>
                            </FormControl>
                        </Paper>
                        {subFormatoTecnico === 'DICTAMEN' && <DictamenFormato solicitarPdf={solicitarPdf} />}
                        {subFormatoTecnico === 'RESGUARDO' && <FormularioResguardo solicitarPdf={solicitarPdf} />}
                    </Box>
                )}

                {/* PESTAÑA 1: Reporte de Actividades */}
                {tabIndex === 1 && <ReporteActividadesFormato solicitarPdf={solicitarPdf} />}
                
                {/* PESTAÑA 2: Mantenimiento Preventivo */}
                {tabIndex === 2 && <MantenimientoPreventivoFormato solicitarPdf={solicitarPdf} usuarioLogueado={user} />}
            </Box>

            {/* MODAL DEL PDF */}
            <Dialog open={openModal} onClose={handleCloseModal} maxWidth="lg" fullWidth>
                <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Typography variant="h6">{nombrePdfActual}</Typography>
                    <IconButton onClick={handleCloseModal}>
                        <CloseIcon />
                    </IconButton>
                </DialogTitle>
                <DialogContent dividers sx={{ height: '80vh', p: 0 }}>
                    {pdfUrl ? (
                        <iframe 
                            src={pdfUrl} 
                            width="100%" 
                            height="100%" 
                            title="Vista previa PDF"
                            style={{ border: 'none' }}
                        />
                    ) : (
                        <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
                            <Typography>Cargando documento...</Typography>
                        </Box>
                    )}
                </DialogContent>
            </Dialog>
        </Box>
    );
}