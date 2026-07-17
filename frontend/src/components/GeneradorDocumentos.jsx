import React, { useState } from 'react';
import { Box, Typography, Paper, Tabs, Tab, FormControl, InputLabel, Select, MenuItem, Dialog, DialogTitle, DialogContent, DialogActions, Button } from '@mui/material';
import api from "../services/api";

// Importaciones (Asegúrate de que las rutas sean correctas según tu estructura)
import DictamenFormato from "./formato/DictamenFormato.jsx";
import FormularioResguardo from "./formato/FormularioResguardo.jsx";
import ReporteActividadesFormato from "./formato/ReporteActividadesFormato.jsx";
import MantenimientoPreventivoFormato from "./formato/MantenimientoPreventivoFormato.jsx";
import EntradaEquipoFormato from "./formato/EntradaEquipoFormato.jsx"; // Asegúrate de crear este archivo
// Si necesitas Memorandum/Requisicion, descomenta o importa aquí:
// import MemorandumFormato from "./formato/MemorandumFormato.jsx"; 

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
            const response = await api.post(`/v1/documentos/${endpoint}`, payload, { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
            setPdfUrl(url);
            setNombrePdfActual(nombreArchivo);
            setOpenModal(true);
        } catch (error) {
            console.error("Error al generar el PDF:", error);
            alert("Hubo un error al generar el documento.");
        }
    };

    const handleCloseModal = () => {
        setOpenModal(false);
        setTimeout(() => window.URL.revokeObjectURL(pdfUrl), 100);
        setPdfUrl("");
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
                    <Tab label="Entrada de Equipo" />
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
                
                {/* PESTAÑA 3: Entrada de Equipo */}
                {tabIndex === 3 && <EntradaEquipoFormato solicitarPdf={solicitarPdf} />}
            </Box>

            {/* MODAL DEL PDF */}
            <Dialog open={openModal} onClose={handleCloseModal} maxWidth="lg" fullWidth>
                {/* ... (Contenido del Dialog sin cambios) ... */}
            </Dialog>
        </Box>
    );
}