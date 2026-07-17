import React, { useState } from 'react';
import { Box, Typography, Paper, Tabs, Tab, FormControl, InputLabel, Select, MenuItem, Dialog, DialogTitle, DialogContent, DialogActions, Button } from '@mui/material';
import api from "../services/api";

import DictamenFormato from "./formato/DictamenFormato.jsx";
import ReporteActividadesFormato from "./formato/ReporteActividadesFormato.jsx";
import MantenimientoPreventivoFormato from "./formato/MantenimientoPreventivoFormato.jsx";
import FormularioResguardo from "./formato/FormularioResguardo.jsx";

const COLOR_GUINDA = '#801A36';

export default function GeneradorDocumentos({user}) {
    // Estado para la pestaña principal
    const [tabIndex, setTabIndex] = useState(0);

    // Estado interno SOLO para la primera pestaña (Formatos que comparten estructura)
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
            alert("Hubo un error al generar el documento. Verifica que el backend esté listo.");
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

            {/* BARRA DE PESTAÑAS PRINCIPAL */}
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
                    <Tab label="Memorándum" />
                    <Tab label="Requisición de Material" />
                    <Tab label="Reporte de Actividades" />
                    <Tab label="Mantenimiento Preventivo" />
                    <Tab label="Entrada de Equipo" />
                </Tabs>
            </Paper>

            <Box sx={{ width: '100%' }}>
                {/* PESTAÑA 0: GRUPO DE FORMATOS TÉCNICOS (Usa el Select) */}
                {tabIndex === 0 && (
                    <Box>
                        <Paper sx={{ p: 3, mb: 3, backgroundColor: '#f9f9f9', borderRadius: 2 }} elevation={0} variant="outlined">
                            <FormControl fullWidth>
                                <InputLabel id="select-subformato-label">Seleccione el Formato Técnico a Generar</InputLabel>
                                <Select
                                    labelId="select-subformato-label"
                                    value={subFormatoTecnico}
                                    label="Seleccione el Formato Técnico a Generar"
                                    onChange={(e) => setSubFormatoTecnico(e.target.value)}
                                    sx={{ backgroundColor: 'white' }}
                                >
                                    <MenuItem value="DICTAMEN">Dictamen Técnico</MenuItem>
                                    <MenuItem value="RESGUARDO">Responsiva de Resguardo de Bienes</MenuItem>
                                    {/* Aquí puedes agregar futuros formatos similares */}
                                </Select>
                            </FormControl>
                        </Paper>

                        {/* Renderiza el componente según lo elegido en el Select */}
                        {subFormatoTecnico === 'DICTAMEN' && <DictamenFormato solicitarPdf={solicitarPdf} />}
                        {subFormatoTecnico === 'RESGUARDO' && <FormularioResguardo solicitarPdf={solicitarPdf} />}
                    </Box>
                )}

                {/* RESTO DE LAS PESTAÑAS INDEPENDIENTES */}
                {tabIndex === 1 && <MemorandumFormato solicitarPdf={solicitarPdf} />}
                {tabIndex === 2 && <RequisicionFormato solicitarPdf={solicitarPdf} />}
                {tabIndex === 3 && <ReporteActividadesFormato solicitarPdf={solicitarPdf} />}
                {tabIndex === 4 && <MantenimientoPreventivoFormato solicitarPdf={solicitarPdf} usuarioLogueado={user} />}
            </Box>

            {/* MODAL DEL PDF (Se mantiene intacto) */}
            <Dialog open={openModal} onClose={handleCloseModal} maxWidth="lg" fullWidth>
                <DialogTitle sx={{ color: COLOR_GUINDA, fontWeight: 'bold' }}>
                    Vista Previa: {nombrePdfActual}
                </DialogTitle>
                <DialogContent dividers sx={{ height: '80vh', p: 0 }}>
                    {pdfUrl && (
                        <iframe
                            src={pdfUrl}
                            width="100%"
                            height="100%"
                            style={{ border: 'none' }}
                            title="Vista previa del documento"
                        />
                    )}
                </DialogContent>
                <DialogActions>
                    <Button onClick={handleCloseModal} color="inherit">Cerrar</Button>
                    <Button 
                        variant="contained" 
                        sx={{ backgroundColor: COLOR_GUINDA, '&:hover': { backgroundColor: '#5c1226' } }} 
                        component="a" 
                        href={pdfUrl} 
                        download={nombrePdfActual}
                    >
                        Descargar Documento
                    </Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
}