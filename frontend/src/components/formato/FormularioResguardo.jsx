import { useState, useContext } from 'react';
import { Box, Typography, TextField, Button, Alert, Paper, MenuItem, Grid } from '@mui/material';
import api from '../../services/api';
import { AuthContext } from '../../context/AuthContext.jsx';
import { toUpper } from '../../util/formater';

const COLOR_GUINDA = '#5c0a28';

export default function FormularioResguardo({ solicitarPdf }) {
    const { user } = useContext(AuthContext);

    const [formData, setFormData] = useState({
        solicitanteNombre: user?.nombre || '',
        solicitanteNumero: '', // <--- AGREGADO: Campo faltante para evitar el error de base de datos
        telefono: '',
        departamento: '',
        equipoNombre: '',
        numeroSerie: '',
        numeroInventario: '',
        condiciones: 'BUENO',
        accesorios: '',
        duracionCantidad: 1,
        duracionTipo: 'PERMANENTE'
    });

    const [mensaje, setMensaje] = useState({ tipo: '', texto: '' });

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: toUpper(value) }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            // Esto ahora enviará solicitanteNumero correctamente
            await api.post('/v1/resguardos', formData);
            await solicitarPdf('resguardos', formData, `Resguardo_${formData.solicitanteNombre}.pdf`);
            setMensaje({ tipo: 'success', texto: 'Resguardo creado y PDF generado con éxito.' });
        } catch (error) {
            console.error("Error al guardar:", error);
            setMensaje({ tipo: 'error', texto: 'Error al guardar el resguardo. Revisa los datos.' });
        }
    };

    return (
        <Box sx={{ p: 3 }}>
            <Paper sx={{ p: 4 }}>
                <Typography variant="h5" sx={{ mb: 3 }}>Nuevo Resguardo</Typography>
                {mensaje.texto && <Alert severity={mensaje.tipo} sx={{ mb: 2 }}>{mensaje.texto}</Alert>}
                
                <form onSubmit={handleSubmit}>
                    <Grid container spacing={2}>
                        <Grid item xs={12} sm={6}>
                            <TextField fullWidth name="solicitanteNombre" label="Nombre del Solicitante" value={formData.solicitanteNombre} onChange={handleChange} required />
                        </Grid>
                        {/* NUEVO CAMPO AGREGADO AQUÍ */}
                        <Grid item xs={12} sm={6}>
                            <TextField fullWidth name="solicitanteNumero" label="No. de Empleado" value={formData.solicitanteNumero} onChange={handleChange} required />
                        </Grid>
                        
                        <Grid item xs={12} sm={6}>
                            <TextField fullWidth name="telefono" label="Teléfono" value={formData.telefono} onChange={handleChange} />
                        </Grid>
                        <Grid item xs={12} sm={6}>
                            <TextField fullWidth name="departamento" label="Departamento" value={formData.departamento} onChange={handleChange} />
                        </Grid>
                        <Grid item xs={12} sm={6}>
                            <TextField fullWidth name="equipoNombre" label="Nombre del Equipo" value={formData.equipoNombre} onChange={handleChange} required />
                        </Grid>
                        <Grid item xs={12} sm={6}>
                            <TextField fullWidth name="numeroSerie" label="No. de Serie" value={formData.numeroSerie} onChange={handleChange} required />
                        </Grid>
                        <Grid item xs={12} sm={3}>
                            <TextField fullWidth name="numeroInventario" label="No. Inventario" value={formData.numeroInventario} onChange={handleChange} />
                        </Grid>
                        <Grid item xs={12} sm={6}>
                            <TextField select fullWidth name="condiciones" label="Condiciones Físicas" value={formData.condiciones} onChange={handleChange}>
                                <MenuItem value="BUENO">BUENO</MenuItem>
                                <MenuItem value="REGULAR">REGULAR</MenuItem>
                                <MenuItem value="MALO">MALO</MenuItem>
                            </TextField>
                        </Grid>
                        <Grid item xs={12} sm={6}>
                            <TextField select fullWidth name="duracionTipo" label="Vigencia" value={formData.duracionTipo} onChange={handleChange}>
                                <MenuItem value="DIAS">DÍAS</MenuItem>
                                <MenuItem value="SEMANAS">SEMANAS</MenuItem>
                                <MenuItem value="PERMANENTE">PERMANENTE</MenuItem>
                            </TextField>
                        </Grid>
                    </Grid>
                    <Button type="submit" variant="contained" sx={{ mt: 3, bgcolor: COLOR_GUINDA }}>Generar Resguardo</Button>
                </form>
            </Paper>
        </Box>
    );
}