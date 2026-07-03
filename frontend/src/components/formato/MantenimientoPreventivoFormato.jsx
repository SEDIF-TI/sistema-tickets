import React, { useState } from 'react';
import { Box, TextField, Button, Grid, Typography, Paper, IconButton, MenuItem } from '@mui/material';

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
        <Box component="form" onSubmit={handleSubmit} sx={{ mt: 2 }}>
            {/* SECCIÓN 1: DATOS GENERALES */}
            <Paper elevation={2} sx={{ p: 3, mb: 3, borderRadius: 2 }}>
                <Typography variant="h6" fontWeight="bold" sx={{ color: COLOR_GUINDA, mb: 2 }}>
                    1. Datos del Periodo y Departamento
                </Typography>
                <Grid container spacing={2}>
                    <Grid item xs={12} sm={4}>
                        <TextField 
                            fullWidth 
                            type="date" 
                            label="Fecha de Inicio" 
                            InputLabelProps={{ shrink: true }} 
                            value={fechaInicio} 
                            onChange={(e) => setFechaInicio(e.target.value)} 
                            required 
                        />
                    </Grid>
                    <Grid item xs={12} sm={4}>
                        <TextField 
                            fullWidth 
                            type="date" 
                            label="Fecha de Fin" 
                            InputLabelProps={{ shrink: true }} 
                            value={fechaFin} 
                            onChange={(e) => setFechaFin(e.target.value)} 
                            required 
                        />
                    </Grid>
                    <Grid item xs={12} sm={4}>
                        <TextField 
                            fullWidth 
                            label="Departamento (Firma Derecha)" 
                            value={departamento} 
                            onChange={handleMayusculas(setDepartamento)} 
                            required 
                        />
                    </Grid>
                </Grid>
            </Paper>

            {/* SECCIÓN 2: LISTA DE EQUIPOS */}
            <Paper elevation={2} sx={{ p: 3, mb: 3, borderRadius: 2 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                    <Typography variant="h6" fontWeight="bold" sx={{ color: COLOR_GUINDA }}>
                        2. Equipos con Mantenimiento Preventivo
                    </Typography>
                    <Button 
                        variant="outlined" 
                        onClick={agregarEquipo}
                        sx={{ color: COLOR_GUINDA, borderColor: COLOR_GUINDA }}
                    >
                        Agregar Equipo
                    </Button>
                </Box>
                
                {equipos.length === 0 ? (
                    <Typography variant="body2" color="textSecondary" align="center" sx={{ py: 3 }}>
                        No hay equipos agregados. Haz clic en "Agregar Equipo" para comenzar.
                    </Typography>
                ) : (
                    equipos.map((equipo, index) => (
                        <Box key={index} sx={{ border: '1px solid #e0e0e0', p: 2, mb: 2, borderRadius: 1, position: 'relative' }}>
                            <Button 
                                color="error" 
                                variant="text"
                                onClick={() => eliminarEquipo(index)}
                                sx={{ position: 'absolute', top: 5, right: 5, fontWeight: 'bold' }}
                            >
                                X
                            </Button>
                            
                            <Typography variant="subtitle2" fontWeight="bold" sx={{ mb: 2, color: '#555' }}>
                                Registro #{index + 1}
                            </Typography>

                            <Grid container spacing={2}>
                                <Grid item xs={12} sm={3}>
                                    <TextField fullWidth size="small" label="Área" value={equipo.area} onChange={(e) => actualizarEquipo(index, 'area', e.target.value)} required />
                                </Grid>
                                <Grid item xs={12} sm={3}>
                                    <TextField fullWidth size="small" label="Usuario Responsable" value={equipo.usuarioResponsable} onChange={(e) => actualizarEquipo(index, 'usuarioResponsable', e.target.value)} required />
                                </Grid>
                                
                                {/* SELECT DEL CATÁLOGO DE EQUIPOS */}
                                <Grid item xs={12} sm={6}>
                                    <TextField 
                                        select 
                                        fullWidth 
                                        size="small" 
                                        label="Seleccionar Catálogo (Autocompleta Marca/Modelo)" 
                                        onChange={(e) => handleSeleccionCatalogo(index, e.target.value)}
                                        defaultValue=""
                                    >
                                        <MenuItem value="" disabled>Seleccione un equipo base...</MenuItem>
                                        {catalogoEquipos.map((cat) => (
                                            <MenuItem key={cat.id} value={cat.id}>
                                                {cat.tipo} - {cat.marca} {cat.modelo}
                                            </MenuItem>
                                        ))}
                                    </TextField>
                                </Grid>

                                <Grid item xs={12} sm={3}>
                                    <TextField fullWidth size="small" label="Tipo CPU" value={equipo.tipoCpu} onChange={(e) => actualizarEquipo(index, 'tipoCpu', e.target.value)} disabled />
                                </Grid>
                                <Grid item xs={12} sm={3}>
                                    <TextField fullWidth size="small" label="Marca" value={equipo.marca} onChange={(e) => actualizarEquipo(index, 'marca', e.target.value)} disabled />
                                </Grid>
                                <Grid item xs={12} sm={3}>
                                    <TextField fullWidth size="small" label="Modelo" value={equipo.modelo} onChange={(e) => actualizarEquipo(index, 'modelo', e.target.value)} disabled />
                                </Grid>

                                <Grid item xs={12} sm={3}>
                                    <TextField fullWidth size="small" label="No. de Serie" value={equipo.numeroSerie} onChange={(e) => actualizarEquipo(index, 'numeroSerie', e.target.value)} required />
                                </Grid>
                                <Grid item xs={12} sm={3}>
                                    <TextField fullWidth size="small" label="Memoria RAM" value={equipo.memoriaRam} onChange={(e) => actualizarEquipo(index, 'memoriaRam', e.target.value)} required />
                                </Grid>
                                <Grid item xs={12} sm={3}>
                                    <TextField fullWidth size="small" label="Capacidad Disco Duro" value={equipo.capacidadDisco} onChange={(e) => actualizarEquipo(index, 'capacidadDisco', e.target.value)} required />
                                </Grid>
                                <Grid item xs={12} sm={6}>
                                    <TextField fullWidth size="small" label="No. de Inventario (Opcional)" value={equipo.numeroInventario} onChange={(e) => actualizarEquipo(index, 'numeroInventario', e.target.value)} />
                                </Grid>
                            </Grid>
                        </Box>
                    ))
                )}
            </Paper>

            <Box textAlign="right">
                <Button 
                    type="submit" 
                    variant="contained" 
                    sx={{ backgroundColor: COLOR_GUINDA, '&:hover': { backgroundColor: '#5c1226' }, px: 4, py: 1.5 }}
                    disabled={equipos.length === 0}
                >
                    Generar Reporte de Mantenimiento
                </Button>
            </Box>
        </Box>
    );
}