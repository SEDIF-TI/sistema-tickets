import React, { useState } from 'react';
import { Box, TextField, Button, Grid, Typography, Paper, IconButton, MenuItem } from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import AddCircleIcon from '@mui/icons-material/AddCircle';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';

const COLOR_GUINDA = '#801A36';

export default function MantenimientoPreventivoFormato({ solicitarPdf, usuarioLogueado }) {
    // Datos Generales
    const [fechaInicio, setFechaInicio] = useState("");
    const [fechaFin, setFechaFin] = useState("");
    const [departamento, setDepartamento] = useState("");
    
    // Lista dinámica de equipos
    const [equipos, setEquipos] = useState([]);

    // Catálogo simulado (esto debería venir de una API real después)
    const [catalogoEquipos] = useState([
        { id: 1, tipo: 'ESCRITORIO', marca: 'DELL', modelo: 'INSPIRON 3647' },
        { id: 2, tipo: 'ALL IN ONE', marca: 'HP', modelo: 'PAVILION 20' },
        { id: 3, tipo: 'ESCRITORIO', marca: 'ACER', modelo: 'ACER POWER FH' }
    ]);

    const handleMayusculas = (setter) => (e) => {
        setter(e.target.value.toUpperCase());
    };

    const agregarEquipo = () => {
        setEquipos([
            ...equipos, 
            { 
                area: '', 
                usuarioResponsable: '', 
                tipoCpu: '', 
                marca: '', 
                modelo: '', 
                numeroSerie: '', 
                memoriaRam: '', 
                capacidadDisco: '', 
                numeroInventario: '' 
            }
        ]);
    };

    const eliminarEquipo = (index) => {
        setEquipos(equipos.filter((_, i) => i !== index));
    };

    const actualizarEquipo = (index, campo, valor) => {
        const nuevaLista = [...equipos];
        nuevaLista[index][campo] = valor.toUpperCase();
        setEquipos(nuevaLista);
    };

    const handleSeleccionCatalogo = (index, equipoId) => {
        const equipoSeleccionado = catalogoEquipos.find(e => e.id === equipoId);
        if (equipoSeleccionado) {
            const nuevaLista = [...equipos];
            nuevaLista[index].tipoCpu = equipoSeleccionado.tipo;
            nuevaLista[index].marca = equipoSeleccionado.marca;
            nuevaLista[index].modelo = equipoSeleccionado.modelo;
            setEquipos(nuevaLista);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        
        const payload = {
            fechaInicio,
            fechaFin,
            nombreTecnico: usuarioLogueado?.nombre || "SOPORTE TÉCNICO",
            departamento,
            equipos
        };

        // DEBUG: Mira esto en la consola del navegador (F12)
        console.log("Payload enviado al servidor:", JSON.stringify(payload, null, 2));

        await solicitarPdf('/v1/documentos/mantenimiento', payload, 'Mantenimiento_Preventivo.pdf');
    };

    return (
        <Box component="form" onSubmit={handleSubmit} sx={{ mt: 2, width: '100%' }}>
            <Paper sx={{ p: { xs: 3, md: 4 }, mb: 3, borderRadius: 2, boxShadow: 2 }}>
                
                <Box sx={{ display: 'flex', flexDirection: { xs: 'column', md: 'row' }, justifyContent: 'space-between', mb: 4 }}>
                    <Box>
                        <Typography variant="h6" sx={{ color: COLOR_GUINDA, fontWeight: 'bold' }}>
                            Reporte de Mantenimiento Preventivo
                        </Typography>
                        <Typography variant="body2" color="textSecondary">
                            Registra el periodo, el departamento y los equipos.
                        </Typography>
                    </Box>
                    <Button variant="outlined" onClick={agregarEquipo} startIcon={<AddCircleIcon />} sx={{ color: COLOR_GUINDA, borderColor: COLOR_GUINDA }}>
                        Agregar Equipo
                    </Button>
                </Box>
                
                <Paper elevation={0} sx={{ bgcolor: '#f8fafc', p: 3, borderRadius: 2, border: '1px solid #e2e8f0', mb: 4 }}>
                    <Grid container spacing={3}>
                        <Grid item xs={12} md={4}>
                            <TextField fullWidth size="small" type="date" label="Fecha Inicio" InputLabelProps={{shrink: true}} value={fechaInicio} onChange={(e) => setFechaInicio(e.target.value)} required />
                        </Grid>
                        <Grid item xs={12} md={4}>
                            <TextField fullWidth size="small" type="date" label="Fecha Fin" InputLabelProps={{shrink: true}} value={fechaFin} onChange={(e) => setFechaFin(e.target.value)} required />
                        </Grid>
                        <Grid item xs={12} md={4}>
                            <TextField fullWidth size="small" label="Departamento" value={departamento} onChange={handleMayusculas(setDepartamento)} required />
                        </Grid>
                    </Grid>
                </Paper>

                <Box>
                    <Typography variant="subtitle1" fontWeight="bold" sx={{ color: COLOR_GUINDA, mb: 2 }}>
                        Equipos Registrados ({equipos.length})
                    </Typography>

                    {equipos.map((equipo, index) => (
                        <Paper key={index} elevation={0} sx={{ p: 3, mb: 3, borderRadius: 2, border: '1px solid #e2e8f0', position: 'relative' }}>
                            <IconButton color="error" onClick={() => eliminarEquipo(index)} sx={{ position: 'absolute', top: 8, right: 8 }}>
                                <DeleteIcon />
                            </IconButton>
                        
                            <Grid container spacing={2}>
                                <Grid item xs={12} sm={6} md={3}>
                                    <TextField fullWidth size="small" label="Área / Cubículo" value={equipo.area} onChange={(e) => actualizarEquipo(index, 'area', e.target.value)} required />
                                </Grid>
                                <Grid item xs={12} sm={6} md={3}>
                                    <TextField fullWidth size="small" label="Usuario Resp." value={equipo.usuarioResponsable} onChange={(e) => actualizarEquipo(index, 'usuarioResponsable', e.target.value)} required />
                                </Grid>
                                <Grid item xs={12} md={6}>
                                    <TextField select fullWidth size="small" label="Catálogo (Autocompletar)" onChange={(e) => handleSeleccionCatalogo(index, e.target.value)} defaultValue="">
                                        {catalogoEquipos.map((cat) => (
                                            <MenuItem key={cat.id} value={cat.id}>{cat.tipo} - {cat.marca} {cat.modelo}</MenuItem>
                                        ))}
                                    </TextField>
                                </Grid>
                                <Grid item xs={12} sm={4} md={3}>
                                    <TextField fullWidth size="small" label="Tipo CPU" value={equipo.tipoCpu} onChange={(e) => actualizarEquipo(index, 'tipoCpu', e.target.value)} disabled />
                                </Grid>
                                <Grid item xs={12} sm={4} md={3}>
                                    <TextField fullWidth size="small" label="Marca" value={equipo.marca} onChange={(e) => actualizarEquipo(index, 'marca', e.target.value)} disabled />
                                </Grid>
                                <Grid item xs={12} sm={4} md={3}>
                                    <TextField fullWidth size="small" label="Modelo" value={equipo.modelo} onChange={(e) => actualizarEquipo(index, 'modelo', e.target.value)} disabled />
                                </Grid>
                                <Grid item xs={12} sm={6} md={3}>
                                    <TextField fullWidth size="small" label="No. Serie" value={equipo.numeroSerie} onChange={(e) => actualizarEquipo(index, 'numeroSerie', e.target.value)} required />
                                </Grid>
                                <Grid item xs={12} sm={6} md={3}>
                                    <TextField fullWidth size="small" label="RAM" value={equipo.memoriaRam} onChange={(e) => actualizarEquipo(index, 'memoriaRam', e.target.value)} required />
                                </Grid>
                                <Grid item xs={12} sm={6} md={3}>
                                    <TextField fullWidth size="small" label="Disco Duro" value={equipo.capacidadDisco} onChange={(e) => actualizarEquipo(index, 'capacidadDisco', e.target.value)} required />
                                </Grid>
                            </Grid>
                        </Paper>
                    ))}
                </Box>

                <Box sx={{ mt: 4, pt: 3, display: 'flex', justifyContent: 'flex-end' }}>
                    <Button type="submit" variant="contained" disabled={equipos.length === 0} sx={{ backgroundColor: COLOR_GUINDA }}>
                        Generar Reporte
                    </Button>
                </Box>
            </Paper>
        </Box>
    );
}