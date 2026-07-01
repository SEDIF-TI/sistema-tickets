import React, { useState } from 'react';
import { Typography, Paper, TextField, Button, Grid, Box, Divider } from '@mui/material';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';

const COLOR_GUINDA = '#801A36';

export default function DictamenFormato({ solicitarPdf }) {
    // 1. Obtenemos al usuario logueado para que firme automáticamente
    const usuarioLogueado = JSON.parse(localStorage.getItem('user')) || {};
    const nombreTecnico = usuarioLogueado.nombre || 'Técnico de Soporte';

    const [dictamen, setDictamen] = useState({
        // Automáticos (Se envían al backend, pero no se editan)
        folioTicket: '', 
        fecha: new Date().toLocaleDateString('es-MX'), 
        
        // Datos del Equipo
        cve: 'OT', descripcionEquipo: '', marca: '', modelo: '', serie: '', noResguardo: '',
        
        // Datos del Usuario (Movido antes del análisis)
        direccionUsuario: '', departamentoUsuario: '', nombreUsuario: '', telefonoUsuario: '', tipoReporte: '',
        
        // Análisis Técnico
        fallaReportada: '', diagnostico: '', hallazgos: '', conclusion: '',

        // Campos eliminados de la vista, pero se envían vacíos para no romper el backend en Java
        concepto: '', observacion: '', 
        
        // Firmas automatizadas e invisibles en el formulario
        realizadoPor: nombreTecnico,
        revisadoPor: 'C. MARCO POLO OLIVARES GONZALEZ',
        recibidoPor: 'DRA. CARMEN GONZÁLEZ SERDÁN'
    });

    const handleChange = (e) => setDictamen({ ...dictamen, [e.target.name]: e.target.value });

    return (
        <Paper sx={{ p: 4, borderRadius: 2, boxShadow: 2 }}>
            {/* --- SECCIÓN 1: DATOS DEL EQUIPO --- */}
            <Typography variant="h6" sx={{ color: COLOR_GUINDA, fontWeight: 'bold', mb: 2 }}>
                1. Datos Generales y del Equipo
            </Typography>
            <Grid container spacing={2}>
                {/* Folio y Fecha Bloqueados y en gris */}
                <Grid item xs={12} sm={3}>
                    <TextField fullWidth size="small" label="Folio Ticket" value="Autogenerado" disabled sx={{ bgcolor: '#f8fafc' }} />
                </Grid>
                <Grid item xs={12} sm={3}>
                    <TextField fullWidth size="small" label="Fecha" value={dictamen.fecha} disabled sx={{ bgcolor: '#f8fafc' }}/>
                </Grid>
                <Grid item xs={12} sm={6}></Grid>

                <Grid item xs={12} sm={2}><TextField fullWidth size="small" label="CVE (Ej: OT)" name="cve" value={dictamen.cve} onChange={handleChange} /></Grid>
                <Grid item xs={12} sm={4}><TextField fullWidth size="small" label="Descripción (Ej: LAPTOP)" name="descripcionEquipo" value={dictamen.descripcionEquipo} onChange={handleChange} /></Grid>
                <Grid item xs={12} sm={3}><TextField fullWidth size="small" label="Marca" name="marca" value={dictamen.marca} onChange={handleChange} /></Grid>
                <Grid item xs={12} sm={3}><TextField fullWidth size="small" label="Modelo" name="modelo" value={dictamen.modelo} onChange={handleChange} /></Grid>
                <Grid item xs={12} sm={6}><TextField fullWidth size="small" label="No. de Serie" name="serie" value={dictamen.serie} onChange={handleChange} /></Grid>
                <Grid item xs={12} sm={6}><TextField fullWidth size="small" label="No. de Resguardo" name="noResguardo" value={dictamen.noResguardo} onChange={handleChange} /></Grid>
            </Grid>

            <Divider sx={{ my: 4 }} />

            {/* --- SECCIÓN 2: DATOS DEL USUARIO (Movido arriba) --- */}
            <Typography variant="h6" sx={{ color: COLOR_GUINDA, fontWeight: 'bold', mb: 2 }}>
                2. Datos del Usuario
            </Typography>
            <Grid container spacing={2}>
                <Grid item xs={12} sm={6}><TextField fullWidth size="small" label="Nombre del Usuario" name="nombreUsuario" value={dictamen.nombreUsuario} onChange={handleChange} /></Grid>
                <Grid item xs={12} sm={6}><TextField fullWidth size="small" label="Teléfono / Ext" name="telefonoUsuario" value={dictamen.telefonoUsuario} onChange={handleChange} /></Grid>
                <Grid item xs={12} sm={6}><TextField fullWidth size="small" label="Dirección" name="direccionUsuario" value={dictamen.direccionUsuario} onChange={handleChange} /></Grid>
                <Grid item xs={12} sm={6}><TextField fullWidth size="small" label="Departamento" name="departamentoUsuario" value={dictamen.departamentoUsuario} onChange={handleChange} /></Grid>
                <Grid item xs={12} sm={6}><TextField fullWidth size="small" label="Tipo de Reporte / Servicio" name="tipoReporte" value={dictamen.tipoReporte} onChange={handleChange} /></Grid>
            </Grid>

            <Divider sx={{ my: 4 }} />

            {/* --- SECCIÓN 3: ANÁLISIS TÉCNICO (Movido abajo) --- */}
            <Typography variant="h6" sx={{ color: COLOR_GUINDA, fontWeight: 'bold', mb: 2 }}>
                3. Análisis Técnico
            </Typography>
            <Grid container spacing={2}>
                <Grid item xs={12}><TextField fullWidth multiline minRows={2} label="Descripción de la Falla" name="fallaReportada" value={dictamen.fallaReportada} onChange={handleChange} /></Grid>
                <Grid item xs={12}><TextField fullWidth multiline minRows={3} label="Diagnóstico Técnico" name="diagnostico" value={dictamen.diagnostico} onChange={handleChange} /></Grid>
                <Grid item xs={12}><TextField fullWidth multiline minRows={2} label="Hallazgos (Estado físico, batería, disco...)" name="hallazgos" value={dictamen.hallazgos} onChange={handleChange} /></Grid>
                <Grid item xs={12}>
                    <TextField 
                        fullWidth multiline minRows={2} 
                        label="Conclusión Final (Max 200 caracteres)" 
                        name="conclusion" value={dictamen.conclusion} onChange={handleChange}
                        inputProps={{ maxLength: 200 }} helperText={`${dictamen.conclusion.length}/200 caracteres`}
                    />
                </Grid>
            </Grid>

            {/* LA SECCIÓN 4 FUE ELIMINADA DEL FORMULARIO VISUAL */}

            <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 4, pt: 2, borderTop: '1px solid #eee' }}>
                <Button 
                    variant="contained" 
                    onClick={() => solicitarPdf('dictamen', dictamen, `Dictamen_Automatico.pdf`)} 
                    startIcon={<PictureAsPdfIcon />} 
                    sx={{ bgcolor: COLOR_GUINDA, '&:hover': { bgcolor: '#5e1227' }, px: 4, py: 1.5, fontWeight: 'bold' }}
                >
                    Generar Dictamen Oficial
                </Button>
            </Box>
        </Paper>
    );
}