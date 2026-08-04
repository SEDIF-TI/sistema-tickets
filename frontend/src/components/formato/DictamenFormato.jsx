import { useState, useEffect } from 'react';
import { Typography, Paper, TextField, Button, Grid, Box, Divider, Autocomplete } from '@mui/material';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import { toUpper } from '../../util/formater';
import api from '../../services/api';


export default function DictamenFormato({ solicitarPdf, generando = false }) {
    // 1. Obtenemos al usuario logueado para que firme automáticamente
    const usuarioLogueado = JSON.parse(localStorage.getItem('user')) || {};
    const nombreTecnico = usuarioLogueado.nombre || 'Técnico de Soporte';

    const [dictamen, setDictamen] = useState({
        // Automáticos (Se envían al backend, pero no se editan)
        folioTicket: '', 
        fecha: new Date().toLocaleDateString('es-MX'), 
        
        // Datos del Equipo (cve restaurado al estado inicial)
        cve: 'OT', descripcionEquipo: '', marca: '', modelo: '', serie: '', noResguardo: '',
        
        // Datos del Usuario
        direccionUsuario: '', departamentoUsuario: '', nombreUsuario: '', telefonoUsuario: '', tipoReporte: '',
        
        // Análisis Técnico
        fallaReportada: '', diagnostico: '', hallazgos: '', conclusion: '',

        // Campos eliminados de la vista, pero se envían vacíos para no romper el backend
        concepto: '', observacion: '', 
        
        // Firmas automatizadas
        realizadoPor: nombreTecnico,
        revisadoPor: 'C. MARCO POLO OLIVARES GONZALEZ',
        recibidoPor: 'DRA. CARMEN GONZÁLEZ SERDÁN'
    });

    // Estados para el Catálogo Inteligente
    const [opcionesEquipo, setOpcionesEquipo] = useState([]);
    const [busquedaEquipo, setBusquedaEquipo] = useState('');

    // Efecto para buscar equipos en tiempo real
    useEffect(() => {
        if (busquedaEquipo.length < 2) {
            setOpcionesEquipo([]);
            return;
        }
        const fetchEquipos = async () => {
            try {
                const res = await api.get(`/v1/equipos/buscar?q=${busquedaEquipo}`);
                setOpcionesEquipo(res.data);
            } catch (error) {
                console.error("Error buscando equipos:", error);
            }
        };
        const timeoutId = setTimeout(() => fetchEquipos(), 300);
        return () => clearTimeout(timeoutId);
    }, [busquedaEquipo]);

    const handleChange = (e) => {
        setDictamen({ ...dictamen, [e.target.name]: toUpper(e.target.value) });
    };

    return (
        <Paper sx={{ p: 4, borderRadius: 2, boxShadow: 2 }}>
            {/* --- SECCIÓN 1: DATOS DEL EQUIPO --- */}
            <Typography variant="h6" sx={{ color: 'primary.main', fontWeight: 'bold', mb: 2 }}>
                1. Datos Generales y del Equipo
            </Typography>
            
            {/* SOLUCIÓN INFALIBLE: Box con CSS Grid nativo forzando el diseño en 2 filas */}
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: 'repeat(1, 1fr)', sm: 'repeat(12, 1fr)' }, gap: 2 }}>
                
                {/* --- PRIMERA FILA --- */}
                <Box sx={{ gridColumn: { xs: 'span 1', sm: 'span 2' } }}>
                    <TextField fullWidth size="small" label="Folio Ticket" value="Autogenerado" disabled sx={{ bgcolor: '#f8fafc' }} />
                </Box>
                
                <Box sx={{ gridColumn: { xs: 'span 1', sm: 'span 2' } }}>
                    <TextField fullWidth size="small" label="Fecha" value={dictamen.fecha} disabled sx={{ bgcolor: '#f8fafc' }}/>
                </Box>
                
                <Box sx={{ gridColumn: { xs: 'span 1', sm: 'span 2' } }}>
                    <TextField fullWidth size="small" label="CVE (Ej: OT)" name="cve" value={dictamen.cve} onChange={handleChange} />
                </Box>

                {/* CAMPO DE DESCRIPCIÓN GIGANTE: Forzado a ocupar la mitad derecha (6 de 12 columnas) */}
                <Box sx={{ gridColumn: { xs: 'span 1', sm: 'span 6' } }}>
                    <Autocomplete
                        freeSolo
                        options={opcionesEquipo}
                        getOptionLabel={(option) => typeof option === 'string' ? option : option.descripcion}
                        inputValue={dictamen.descripcionEquipo}
                        onInputChange={(event, newInputValue) => {
                            setBusquedaEquipo(newInputValue);
                            setDictamen({ ...dictamen, descripcionEquipo: toUpper(newInputValue) });
                        }}
                        onChange={(event, newValue) => {
                            if (typeof newValue === 'object' && newValue !== null) {
                                setDictamen({
                                    ...dictamen,
                                    descripcionEquipo: newValue.descripcion || '',
                                    marca: newValue.marca || '',
                                    modelo: newValue.modelo || ''
                                });
                            } else if (typeof newValue === 'string') {
                                setDictamen({ ...dictamen, descripcionEquipo: toUpper(newValue) });
                            }
                        }}
                        renderInput={(params) => (
                            <TextField {...params} fullWidth size="small" label="Descripción (Catálogo)" />
                        )}
                    />
                </Box>

                {/* --- SEGUNDA FILA --- */}
                <Box sx={{ gridColumn: { xs: 'span 1', sm: 'span 3' } }}>
                    <TextField fullWidth size="small" label="Marca" name="marca" value={dictamen.marca} onChange={handleChange} />
                </Box>
                
                <Box sx={{ gridColumn: { xs: 'span 1', sm: 'span 3' } }}>
                    <TextField fullWidth size="small" label="Modelo" name="modelo" value={dictamen.modelo} onChange={handleChange} />
                </Box>
                
                <Box sx={{ gridColumn: { xs: 'span 1', sm: 'span 3' } }}>
                    <TextField fullWidth size="small" label="No. de Serie" name="serie" value={dictamen.serie} onChange={handleChange} />
                </Box>
                
                <Box sx={{ gridColumn: { xs: 'span 1', sm: 'span 3' } }}>
                    <TextField fullWidth size="small" label="No. de Resguardo" name="noResguardo" value={dictamen.noResguardo} onChange={handleChange} />
                </Box>

            </Box>

            <Divider sx={{ my: 4 }} />

            {/* --- SECCIÓN 2: DATOS DEL USUARIO --- */}
            <Typography variant="h6" sx={{ color: 'primary.main', fontWeight: 'bold', mb: 2 }}>
                2. Datos del Usuario
            </Typography>
            <Grid container spacing={2}>
                <Grid size={{ xs: 12, sm: 6 }}><TextField fullWidth size="small" label="Nombre del Usuario" name="nombreUsuario" value={dictamen.nombreUsuario} onChange={handleChange} /></Grid>
                <Grid size={{ xs: 12, sm: 6 }}><TextField fullWidth size="small" label="Teléfono / Ext" name="telefonoUsuario" value={dictamen.telefonoUsuario} onChange={handleChange} /></Grid>
                <Grid size={{ xs: 12, sm: 6 }}><TextField fullWidth size="small" label="Dirección" name="direccionUsuario" value={dictamen.direccionUsuario} onChange={handleChange} /></Grid>
                <Grid size={{ xs: 12, sm: 6 }}><TextField fullWidth size="small" label="Departamento" name="departamentoUsuario" value={dictamen.departamentoUsuario} onChange={handleChange} /></Grid>
                <Grid size={{ xs: 12, sm: 6 }}><TextField fullWidth size="small" label="Tipo de Reporte / Servicio" name="tipoReporte" value={dictamen.tipoReporte} onChange={handleChange} /></Grid>
            </Grid>

            <Divider sx={{ my: 4 }} />

            {/* --- SECCIÓN 3: ANÁLISIS TÉCNICO --- */}
            <Typography variant="h6" sx={{ color: 'primary.main', fontWeight: 'bold', mb: 2 }}>
                3. Análisis Técnico
            </Typography>
            <Grid container spacing={2}>
                <Grid size={{ xs: 12 }}><TextField fullWidth multiline minRows={2} label="Descripción de la Falla" name="fallaReportada" value={dictamen.fallaReportada} onChange={handleChange} /></Grid>
                <Grid size={{ xs: 12 }}><TextField fullWidth multiline minRows={3} label="Diagnóstico Técnico" name="diagnostico" value={dictamen.diagnostico} onChange={handleChange} /></Grid>
                <Grid size={{ xs: 12 }}><TextField fullWidth multiline minRows={2} label="Hallazgos (Estado físico, batería, disco...)" name="hallazgos" value={dictamen.hallazgos} onChange={handleChange} /></Grid>
                <Grid size={{ xs: 12 }}>
                    <TextField 
                        fullWidth multiline minRows={2} 
                        label="Conclusión Final (Max 200 caracteres)" 
                        name="conclusion" value={dictamen.conclusion} onChange={handleChange}
                        InputProps={{ maxLength: 200 }} helperText={`${dictamen.conclusion.length}/200 caracteres`}
                    />
                </Grid>
            </Grid>

            <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 4, pt: 2, borderTop: '1px solid #eee' }}>
                <Button disabled={generando} 
                    variant="contained" 
                    onClick={async () => {
                        try {
                            if (dictamen.descripcionEquipo) {
                                await api.post('/v1/equipos/upsert', {
                                    descripcion: dictamen.descripcionEquipo,
                                    marca: dictamen.marca,
                                    modelo: dictamen.modelo
                                });
                            }
                        } catch (e) { console.error("No se pudo actualizar catálogo JIT", e); }
                        
                        // --- CÓDIGO NUEVO PARA AGREGAR "C. " ---
                        // Creamos una copia de los datos pero formateando los nombres
                        const datosParaPdf = {
                            ...dictamen,
                            // Agregamos "C. " al técnico (REALIZADO POR) si no lo tiene ya
                            realizadoPor: dictamen.realizadoPor.startsWith('C. ') ? dictamen.realizadoPor : `C. ${dictamen.realizadoPor}`,
                            // Agregamos "C. " al usuario (RECIBIDO POR) si no lo tiene ya
                            nombreUsuario: dictamen.nombreUsuario && !dictamen.nombreUsuario.startsWith('C. ') 
                                            ? `C. ${dictamen.nombreUsuario}` 
                                            : dictamen.nombreUsuario
                        };

                        // Enviamos la copia formateada en lugar del estado original
                        solicitarPdf('dictamen', datosParaPdf, `Dictamen_Automatico.pdf`);
                    }} 
                    startIcon={<PictureAsPdfIcon />} 
                    sx={{ bgcolor: 'primary.main', '&:hover': { bgcolor: '#5e1227' }, px: 4, py: 1.5, fontWeight: 'bold' }}
                >
                    Generar Dictamen Oficial
                </Button>
            </Box>
        </Paper>
    );
}