import React, { useState } from 'react';
import { Box, Typography, Paper, Tabs, Tab } from '@mui/material';
import api from "../services/api";

// Importamos los submódulos con la extensión .jsx y la carpeta en singular
import DictamenFormato from "./formato/DictamenFormato.jsx";
import MemorandumFormato from "./formato/MemorandumFormato.jsx";
import RequisicionFormato from "./formato/RequisicionFormato.jsx";
import ReporteActividadesFormato from "./formato/ReporteActividadesFormato.jsx";

const COLOR_GUINDA = '#801A36';

export default function GeneradorDocumentos() {
    const [tabIndex, setTabIndex] = useState(0);

    // Mantenemos la función de descarga aquí para no repetirla en cada archivo
    const solicitarPdf = async (endpoint, payload, nombreArchivo) => {
        try {
            const response = await api.post(`/v1/documentos/${endpoint}`, payload, { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', nombreArchivo);
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch (error) {
            console.error("Error al generar el PDF:", error);
            alert("Hubo un error al generar el documento. Verifica que el backend esté listo.");
        }
    };

    return (
        <Box sx={{ p: 3, maxWidth: 1000, mx: 'auto' }}>
            <Typography variant="h5" fontWeight="bold" sx={{ color: COLOR_GUINDA, mb: 3 }}>
                Generador de Documentos Oficiales
            </Typography>

            <Paper sx={{ mb: 3, borderRadius: 2, overflow: 'hidden' }}>
                <Tabs 
                    value={tabIndex} 
                    onChange={(e, newValue) => setTabIndex(newValue)} 
                    centered 
                    TabIndicatorProps={{ style: { backgroundColor: COLOR_GUINDA } }}
                    sx={{ '& .Mui-selected': { color: `${COLOR_GUINDA} !important`, fontWeight: 'bold' } }}
                >
                    <Tab label="Dictamen Técnico" />
                    <Tab label="Memorándum" />
                    <Tab label="Requisición de Material" />
                    <Tab label="Reporte de Actividades" />
                </Tabs>
            </Paper>

            {/* Renderizado condicional de los componentes hijos */}
            {tabIndex === 0 && <DictamenFormato solicitarPdf={solicitarPdf} />}
            {tabIndex === 1 && <MemorandumFormato solicitarPdf={solicitarPdf} />}
            {tabIndex === 2 && <RequisicionFormato solicitarPdf={solicitarPdf} />}
            {tabIndex === 3 && <ReporteActividadesFormato solicitarPdf={solicitarPdf} />}
        </Box>
    );
}