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

    // Simulación del catálogo de equipos
    const [catalogoEquipos, setCatalogoEquipos] = useState([
        { id: 1, tipo: 'ESCRITORIO', marca: 'DELL', modelo: 'INSPIRON 3647' },
        { id: 2, tipo: 'ALL IN ONE', marca: 'HP', modelo: 'PAVILION 20' },
        { id: 3, tipo: 'ESCRITORIO', marca: 'ACER', modelo: 'ACER POWER FH' }
    ]);

    // Función para forzar mayúsculas en campos de texto normales
    const handleMayusculas = (setter) => (e) => {
        setter(e.target.value.toUpperCase());
    };

    // Funciones para la tabla dinámica
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
        const nuevaLista = equipos.filter((_, i) => i !== index);
        setEquipos(nuevaLista);
    };

    const actualizarEquipo = (index, campo, valor) => {
        const nuevaLista = [...equipos];
        // Convertimos a mayúsculas todo lo que el usuario escriba en la tabla
        nuevaLista[index][campo] = valor.toUpperCase();
        setEquipos(nuevaLista);
    };

    // Cuando selecciona un equipo del catálogo, autocompletamos Marca, Modelo y Tipo
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

    const handleSubmit = (e) => {
        e.preventDefault();
        
        // Armamos el payload exacto para Spring Boot
        const payload = {
            fechaInicio,
            fechaFin,
            nombreTecnico: usuarioLogueado?.nombre || "SOPORTE TÉCNICO",
            departamento,
            equipos
        };

        solicitarPdf('mantenimiento', payload, 'Mantenimiento_Preventivo.pdf');
    };

    return (
        <Box component="form" onSubmit={handleSubmit}>
            <Paper sx={{ p: { xs: 3, md: 4 }, borderRadius: 2, boxShadow: 2, width: '100%', boxSizing: 'border-box' }}>
                
                {/* ENCABEZADO DIVIDIDO */}
                <Box sx={{ display: 'flex', flexDirection: { xs: 'column', md: 'row' }, justifyContent: 'space-between', alignItems: { xs: 'flex-start', md: 'center' }, mb: 4, gap: 2 }}>
                    <Box>
                        <Typography variant="h6" sx={{ color: COLOR_GUINDA, fontWeight: 'bold' }}>
                            Reporte de Mantenimiento Preventivo
                        </Typography>
                        <Typography variant="body2" color="textSecondary">
                            Registra el periodo, el departamento y los equipos a los que se les realizó mantenimiento.
                        </Typography>
                    </Box>
                    <Button 
                        variant="outlined" 
                        onClick={agregarEquipo}
                        startIcon={<AddCircleIcon />}
                        sx={{ color: COLOR_GUINDA, borderColor: COLOR_GUINDA, '&:hover': { bgcolor: '#fce4ec', borderColor: COLOR_GUINDA }, fontWeight: 'bold', minWidth: 'max-content' }}
                    >
                        Agregar Equipo
                    </Button>
                </Box>

                {/* BARRA DE DATOS GENERALES (TOOLBAR) */}
                <Paper elevation={0} sx={{ bgcolor: '#f8fafc', p: 3, borderRadius: 2, border: '1px solid #e2e8f0', mb: 4 }}>
                    <Grid container spacing={3}>
                        <Grid item xs={12} md={4}>
                            <Typography variant="caption" color="textSecondary" sx={{ display: 'block', mb: 1, fontWeight: 'bold', ml: 0.5 }}>
                                Fecha de Inicio *
                            </Typography>
                            <TextField 
                                fullWidth 
                                size="small"
                                type="date" 
                                value={fechaInicio} 
                                onChange={(e) => setFechaInicio(e.target.value)} 
                                sx={{ bgcolor: 'white' }}
                                required 
                            />
                        </Grid>
                        <Grid item xs={12} md={4}>
                            <Typography variant="caption" color="textSecondary" sx={{ display: 'block', mb: 1, fontWeight: 'bold', ml: 0.5 }}>
                                Fecha de Fin *
                            </Typography>
                            <TextField 
                                fullWidth 
                                size="small"
                                type="date" 
                                value={fechaFin} 
                                onChange={(e) => setFechaFin(e.target.value)} 
                                sx={{ bgcolor: 'white' }}
                                required 
                            />
                        </Grid>
                        <Grid item xs={12} md={4}>
                            <Typography variant="caption" color="textSecondary" sx={{ display: 'block', mb: 1, fontWeight: 'bold', ml: 0.5 }}>
                                Departamento (Firma Derecha) *
                            </Typography>
                            <TextField 
                                fullWidth 
                                size="small"
                                placeholder="Ej. RECURSOS HUMANOS"
                                value={departamento} 
                                onChange={handleMayusculas(setDepartamento)} 
                                sx={{ bgcolor: 'white' }}
                                required 
                            />
                        </Grid>
                    </Grid>
                </Paper>

                {/* LISTA DINÁMICA DE EQUIPOS */}
                <Box>
                    <Typography variant="subtitle1" fontWeight="bold" sx={{ color: COLOR_GUINDA, mb: 2 }}>
                        Equipos Registrados ({equipos.length})
                    </Typography>

                    {equipos.length === 0 ? (
                        <Box sx={{ p: 4, textAlign: 'center', bgcolor: '#fbfbfb', borderRadius: 2, border: '1px dashed #ccc' }}>
                            <Typography variant="body1" color="textSecondary">
                                No hay equipos agregados en este reporte.
                            </Typography>
                            <Typography variant="body2" color="textSecondary" sx={{ mt: 1 }}>
                                Haz clic en el botón superior "Agregar Equipo" para comenzar.
                            </Typography>
                        </Box>
                    ) : (
                        equipos.map((equipo, index) => (
                            <Paper key={index} elevation={0} sx={{ p: 3, mb: 3, borderRadius: 2, border: '1px solid #e2e8f0', position: 'relative' }}>
                                
                                {/* Botón de eliminar sutil en la esquina superior derecha */}
                                <IconButton 
                                    color="error" 
                                    onClick={() => eliminarEquipo(index)}
                                    sx={{ position: 'absolute', top: 8, right: 8 }}
                                    title="Eliminar este equipo"
                                >
                                    <DeleteIcon />
                                </IconButton>
                                
                                <Typography variant="subtitle2" fontWeight="bold" sx={{ mb: 3, color: '#555', borderBottom: '1px solid #eee', pb: 1, width: 'fit-content' }}>
                                    Equipo #{index + 1}
                                </Typography>

                                <Grid container spacing={2}>
                                    <Grid item xs={12} sm={6} md={3}>
                                        <TextField fullWidth size="small" label="Área / Cubículo" value={equipo.area} onChange={(e) => actualizarEquipo(index, 'area', e.target.value)} required />
                                    </Grid>
                                    <Grid item xs={12} sm={6} md={3}>
                                        <TextField fullWidth size="small" label="Usuario Responsable" value={equipo.usuarioResponsable} onChange={(e) => actualizarEquipo(index, 'usuarioResponsable', e.target.value)} required />
                                    </Grid>
                                    
                                    {/* SELECT DEL CATÁLOGO DE EQUIPOS */}
                                    <Grid item xs={12} md={6}>
                                        <TextField 
                                            select 
                                            fullWidth 
                                            size="small" 
                                            label="Seleccionar Catálogo (Autocompleta Marca/Modelo)" 
                                            onChange={(e) => handleSeleccionCatalogo(index, e.target.value)}
                                            defaultValue=""
                                            sx={{ bgcolor: '#f8fafc' }}
                                        >
                                            <MenuItem value="" disabled>Seleccione un equipo base...</MenuItem>
                                            {catalogoEquipos.map((cat) => (
                                                <MenuItem key={cat.id} value={cat.id}>
                                                    {cat.tipo} - {cat.marca} {cat.modelo}
                                                </MenuItem>
                                            ))}
                                        </TextField>
                                    </Grid>

                                    {/* CAMPOS AUTOCOMPLETADOS (Deshabilitados) */}
                                    <Grid item xs={12} sm={4} md={3}>
                                        <TextField fullWidth size="small" label="Tipo CPU" value={equipo.tipoCpu} onChange={(e) => actualizarEquipo(index, 'tipoCpu', e.target.value)} disabled sx={{ bgcolor: '#f5f5f5' }} />
                                    </Grid>
                                    <Grid item xs={12} sm={4} md={3}>
                                        <TextField fullWidth size="small" label="Marca" value={equipo.marca} onChange={(e) => actualizarEquipo(index, 'marca', e.target.value)} disabled sx={{ bgcolor: '#f5f5f5' }} />
                                    </Grid>
                                    <Grid item xs={12} sm={4} md={3}>
                                        <TextField fullWidth size="small" label="Modelo" value={equipo.modelo} onChange={(e) => actualizarEquipo(index, 'modelo', e.target.value)} disabled sx={{ bgcolor: '#f5f5f5' }} />
                                    </Grid>

                                    {/* CARACTERÍSTICAS TÉCNICAS */}
                                    <Grid item xs={12} sm={6} md={3}>
                                        <TextField fullWidth size="small" label="No. de Serie" value={equipo.numeroSerie} onChange={(e) => actualizarEquipo(index, 'numeroSerie', e.target.value)} required />
                                    </Grid>
                                    <Grid item xs={12} sm={6} md={3}>
                                        <TextField fullWidth size="small" label="Memoria RAM" value={equipo.memoriaRam} onChange={(e) => actualizarEquipo(index, 'memoriaRam', e.target.value)} required />
                                    </Grid>
                                    <Grid item xs={12} sm={6} md={3}>
                                        <TextField fullWidth size="small" label="Capacidad Disco Duro" value={equipo.capacidadDisco} onChange={(e) => actualizarEquipo(index, 'capacidadDisco', e.target.value)} required />
                                    </Grid>
                                    <Grid item xs={12} md={6}>
                                        <TextField fullWidth size="small" label="No. de Inventario (Opcional)" value={equipo.numeroInventario} onChange={(e) => actualizarEquipo(index, 'numeroInventario', e.target.value)} />
                                    </Grid>
                                </Grid>
                            </Paper>
                        ))
                    )}
                </Box>

                {/* BOTÓN INFERIOR DE ACCIÓN */}
                <Box sx={{ borderTop: '1px solid #eee', mt: 4, pt: 3, display: 'flex', justifyContent: 'flex-end' }}>
                    <Button 
                        type="submit" 
                        variant="contained" 
                        startIcon={<PictureAsPdfIcon />}
                        sx={{ backgroundColor: COLOR_GUINDA, '&:hover': { backgroundColor: '#5c1226' }, px: 4, py: 1.5, fontWeight: 'bold' }}
                        disabled={equipos.length === 0}
                    >
                        Generar Reporte de Mantenimiento
                    </Button>
                </Box>
            </Paper>
        </Box>
    );
}