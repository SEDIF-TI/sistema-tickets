import React, { useState } from 'react';
// Importamos los componentes del Dialog de Material UI
import { Box, Typography, Paper, Tabs, Tab, Dialog, DialogTitle, DialogContent, DialogActions, Button } from '@mui/material';
import api from "../services/api";

import DictamenFormato from "./formato/DictamenFormato.jsx";
import ReporteActividadesFormato from "./formato/ReporteActividadesFormato.jsx";
import MantenimientoPreventivoFormato from "./formato/MantenimientoPreventivoFormato.jsx";
import EntradaEquipoFormato from "./formato/EntradaEquipoFormato.jsx";

const COLOR_GUINDA = '#801A36';

export default function GeneradorDocumentos({user}) {
    const [tabIndex, setTabIndex] = useState(0);

    // --- ESTADOS PARA CONTROLAR EL MODAL ---
    const [openModal, setOpenModal] = useState(false);
    const [pdfUrl, setPdfUrl] = useState("");
    const [nombrePdfActual, setNombrePdfActual] = useState("");

    const solicitarPdf = async (endpoint, payload, nombreArchivo) => {
        try {
            const response = await api.post(`/v1/documentos/${endpoint}`, payload, { responseType: 'blob' });
            
            // Creamos la URL temporal
            const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
            
            // Guardamos los datos y ABRIMOS EL MODAL
            setPdfUrl(url);
            setNombrePdfActual(nombreArchivo);
            setOpenModal(true);

        } catch (error) {
            console.error("Error al generar el PDF:", error);
            alert("Hubo un error al generar el documento. Verifica que el backend esté listo.");
        }
    };

    // Función para cerrar el modal y limpiar la memoria
    const handleCloseModal = () => {
        setOpenModal(false);
        // Limpiamos la URL de la memoria para que no sature el navegador
        setTimeout(() => window.URL.revokeObjectURL(pdfUrl), 100);
        setPdfUrl("");
    };

    return (
        /* CORRECCIÓN: Quitamos maxWidth: 1000 y mx: 'auto' para que use todo el ancho disponible */
        <Box sx={{ p: 3, width: '100%', boxSizing: 'border-box' }}>
            <Typography variant="h5" fontWeight="bold" sx={{ color: COLOR_GUINDA, mb: 3 }}>
                Generador de Documentos Oficiales
            </Typography>

            <Paper sx={{ mb: 3, borderRadius: 2, overflow: 'hidden', width: '100%' }}>
                <Tabs 
                    value={tabIndex} 
                    onChange={(e, newValue) => setTabIndex(newValue)} 
                    centered 
                    variant="scrollable"
                    scrollButtons="auto"
                    TabIndicatorProps={{ style: { backgroundColor: COLOR_GUINDA } }}
                    sx={{ '& .Mui-selected': { color: `${COLOR_GUINDA} !important`, fontWeight: 'bold' } }}
                >
                    <Tab label="Dictamen Técnico" />
                    <Tab label="Reporte de Actividades" />
                    <Tab label="Mantenimiento Preventivo" />
                    <Tab label="Entrada de Equipo" />
                </Tabs>
            </Paper>

            {/* Renderizado condicional de los componentes hijos */}
            {tabIndex === 0 && <DictamenFormato solicitarPdf={solicitarPdf} />}
            {tabIndex === 1 && <ReporteActividadesFormato solicitarPdf={solicitarPdf} />}
            {tabIndex === 2 && <MantenimientoPreventivoFormato solicitarPdf={solicitarPdf} usuarioLogueado={user} />}
            {tabIndex === 3 && <EntradaEquipoFormato solicitarPdf={solicitarPdf} usuarioLogueado={user} />}

            {/* ==========================================
                COMPONENTE MODAL PARA VISUALIZAR EL PDF
                ========================================== */}
            <Dialog
                open={openModal}
                onClose={handleCloseModal}
                maxWidth="lg" 
                fullWidth
            >
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
                    <Button onClick={handleCloseModal} color="inherit">
                        Cerrar
                    </Button>
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