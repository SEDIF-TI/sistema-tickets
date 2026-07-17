import React, { useState } from 'react';
import { Box, TextField, Button, Grid, Typography, Paper, Divider } from '@mui/material';

const COLOR_GUINDA = '#801A36';

export default function EntradaEquipoFormato({ solicitarPdf, usuarioLogueado }) {
    // 1. Estados para Datos Generales
    const [fecha, setFecha] = useState("");
    const [folioTicket, setFolioTicket] = useState("");

    // 2. Estado para Lista de Equipos
    const [equipos, setEquipos] = useState([]);

    // 3. Estados para Ubicaciones
    const [ubicacionActual, setUbicacionActual] = useState({
        departamentoActual: '', direccionActual: '', telefonoActual: '', responsableActual: ''
    });
    const [ubicacionDestino, setUbicacionDestino] = useState({
        resguardoDestino: '', direccionDestino: '', telefonoDestino: '', responsableDestino: ''
    });

    // 4. Estados para Notas
    const [notas, setNotas] = useState({ concepto: '', observacion: '' });

    // Funciones manejadoras de estado (forzando mayúsculas)
    const handleActualChange = (e) => setUbicacionActual({ ...ubicacionActual, [e.target.name]: e.target.value.toUpperCase() });
    const handleDestinoChange = (e) => setUbicacionDestino({ ...ubicacionDestino, [e.target.name]: e.target.value.toUpperCase() });
    const handleNotasChange = (e) => setNotas({ ...notas, [e.target.name]: e.target.value.toUpperCase() });

    // Funciones para la tabla dinámica
    const agregarEquipo = () => {
        setEquipos([...equipos, { cve: '', descripcion: '', marca: '', modelo: '', serie: '', noResguardo: '', cantidad: '1' }]);
    };
    const eliminarEquipo = (index) => {
        setEquipos(equipos.filter((_, i) => i !== index));
    };
    const actualizarEquipo = (index, campo, valor) => {
        const nuevaLista = [...equipos];
        nuevaLista[index][campo] = valor.toUpperCase();
        setEquipos(nuevaLista);
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        const payload = {
            fecha,
            folioTicket: folioTicket ? parseInt(folioTicket) : null,
            equipos,
            ...ubicacionActual,
            ...ubicacionDestino,
            ...notas,
            entregadoPor: usuarioLogueado?.nombre || "SOPORTE TÉCNICO",
            recibidoPor: "" // El receptor firma físicamente
        };
        solicitarPdf('entrada-equipo', payload, 'Entrada_Equipo.pdf');
    };

    return (
        <Box component="form" onSubmit={handleSubmit} sx={{ mt: 2 }}>
            {/* SECCIÓN 1: DATOS GENERALES */}
            <Paper elevation={2} sx={{ p: 3, mb: 3, borderRadius: 2 }}>
                <Typography variant="h6" fontWeight="bold" sx={{ color: COLOR_GUINDA, mb: 2 }}>
                    1. Datos Generales
                </Typography>
                <Grid container spacing={2}>
                    <Grid item xs={12} sm={6}>
                        <Typography variant="caption" sx={{ color: '#555', fontWeight: 'bold', display: 'block', mb: 0.5 }}>
                            Fecha *
                        </Typography>
                        <TextField 
                            fullWidth type="date" value={fecha} 
                            onChange={(e) => setFecha(e.target.value)} required size="small"
                        />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                        <Typography variant="caption" sx={{ color: '#555', fontWeight: 'bold', display: 'block', mb: 0.5 }}>
                            No. de Folio
                        </Typography>
                        <TextField 
                            fullWidth type="number" value={folioTicket} 
                            onChange={(e) => setFolioTicket(e.target.value)} size="small"
                        />
                    </Grid>
                </Grid>
            </Paper>

            {/* SECCIÓN 2: LISTA DE EQUIPOS */}
            <Paper elevation={2} sx={{ p: 3, mb: 3, borderRadius: 2 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                    <Typography variant="h6" fontWeight="bold" sx={{ color: COLOR_GUINDA }}>
                        2. Detalle de Equipos
                    </Typography>
                    <Button variant="outlined" onClick={agregarEquipo} sx={{ color: COLOR_GUINDA, borderColor: COLOR_GUINDA }}>
                        Agregar Equipo
                    </Button>
                </Box>
                
                {equipos.length === 0 ? (
                    <Typography variant="body2" color="textSecondary" align="center" sx={{ py: 3 }}>
                        No hay equipos agregados. Haga clic en "Agregar Equipo" para comenzar.
                    </Typography>
                ) : (
                    equipos.map((equipo, index) => (
                        <Box key={index} sx={{ border: '1px solid #e0e0e0', p: 2, mb: 2, borderRadius: 1, position: 'relative' }}>
                            <Button color="error" variant="text" onClick={() => eliminarEquipo(index)} sx={{ position: 'absolute', top: 5, right: 5, fontWeight: 'bold' }}>
                                X
                            </Button>
                            <Typography variant="subtitle2" fontWeight="bold" sx={{ mb: 2, color: '#555' }}>
                                Registro #{index + 1}
                            </Typography>
                            <Grid container spacing={2}>
                                <Grid item xs={12} sm={2}><TextField fullWidth size="small" label="CVE" value={equipo.cve} onChange={(e) => actualizarEquipo(index, 'cve', e.target.value)} /></Grid>
                                <Grid item xs={12} sm={10}><TextField fullWidth size="small" label="Descripción" value={equipo.descripcion} onChange={(e) => actualizarEquipo(index, 'descripcion', e.target.value)} required /></Grid>
                                <Grid item xs={12} sm={3}><TextField fullWidth size="small" label="Marca" value={equipo.marca} onChange={(e) => actualizarEquipo(index, 'marca', e.target.value)} /></Grid>
                                <Grid item xs={12} sm={3}><TextField fullWidth size="small" label="Modelo" value={equipo.modelo} onChange={(e) => actualizarEquipo(index, 'modelo', e.target.value)} /></Grid>
                                <Grid item xs={12} sm={4}><TextField fullWidth size="small" label="No. Serie" value={equipo.serie} onChange={(e) => actualizarEquipo(index, 'serie', e.target.value)} /></Grid>
                                <Grid item xs={12} sm={2}><TextField fullWidth size="small" type="number" label="Cantidad" value={equipo.cantidad} onChange={(e) => actualizarEquipo(index, 'cantidad', e.target.value)} required /></Grid>
                                <Grid item xs={12} sm={12}><TextField fullWidth size="small" label="No. de Resguardo" value={equipo.noResguardo} onChange={(e) => actualizarEquipo(index, 'noResguardo', e.target.value)} /></Grid>
                            </Grid>
                        </Box>
                    ))
                )}
            </Paper>

            {/* SECCIÓN 3: UBICACIONES */}
            <Paper elevation={2} sx={{ p: 3, mb: 3, borderRadius: 2 }}>
                <Typography variant="h6" fontWeight="bold" sx={{ color: COLOR_GUINDA, mb: 2 }}>
                    3. Información de Ubicación
                </Typography>
                <Grid container spacing={4}>
                    <Grid item xs={12} md={6}>
                        <Typography variant="subtitle1" fontWeight="bold" sx={{ mb: 1 }}>Ubicación Actual</Typography>
                        <Divider sx={{ mb: 2 }} />
                        <Grid container spacing={2}>
                            <Grid item xs={12}><TextField fullWidth size="small" label="Departamento" name="departamentoActual" value={ubicacionActual.departamentoActual} onChange={handleActualChange} /></Grid>
                            <Grid item xs={12}><TextField fullWidth size="small" label="Dirección" name="direccionActual" value={ubicacionActual.direccionActual} onChange={handleActualChange} /></Grid>
                            <Grid item xs={12}><TextField fullWidth size="small" label="Teléfono" name="telefonoActual" value={ubicacionActual.telefonoActual} onChange={handleActualChange} /></Grid>
                            <Grid item xs={12}><TextField fullWidth size="small" label="Responsable" name="responsableActual" value={ubicacionActual.responsableActual} onChange={handleActualChange} /></Grid>
                        </Grid>
                    </Grid>
                    <Grid item xs={12} md={6}>
                        <Typography variant="subtitle1" fontWeight="bold" sx={{ mb: 1 }}>Ubicación Destino</Typography>
                        <Divider sx={{ mb: 2 }} />
                        <Grid container spacing={2}>
                            <Grid item xs={12}><TextField fullWidth size="small" label="A resguardo de" name="resguardoDestino" value={ubicacionDestino.resguardoDestino} onChange={handleDestinoChange} /></Grid>
                            <Grid item xs={12}><TextField fullWidth size="small" label="Dirección" name="direccionDestino" value={ubicacionDestino.direccionDestino} onChange={handleDestinoChange} /></Grid>
                            <Grid item xs={12}><TextField fullWidth size="small" label="Teléfono" name="telefonoDestino" value={ubicacionDestino.telefonoDestino} onChange={handleDestinoChange} /></Grid>
                            <Grid item xs={12}><TextField fullWidth size="small" label="Responsable" name="responsableDestino" value={ubicacionDestino.responsableDestino} onChange={handleDestinoChange} /></Grid>
                        </Grid>
                    </Grid>
                </Grid>
            </Paper>

            {/* SECCIÓN 4: NOTAS */}
            <Paper elevation={2} sx={{ p: 3, mb: 3, borderRadius: 2 }}>
                <Typography variant="h6" fontWeight="bold" sx={{ color: COLOR_GUINDA, mb: 2 }}>
                    4. Notas Adicionales
                </Typography>
                <Grid container spacing={2}>
                    <Grid item xs={12} sm={6}>
                        <TextField fullWidth multiline rows={3} label="Concepto" name="concepto" value={notas.concepto} onChange={handleNotasChange} />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                        <TextField fullWidth multiline rows={3} label="Observación" name="observacion" value={notas.observacion} onChange={handleNotasChange} />
                    </Grid>
                </Grid>
            </Paper>

            <Box textAlign="right">
                <Button type="submit" variant="contained" sx={{ backgroundColor: COLOR_GUINDA, '&:hover': { backgroundColor: '#5c1226' }, px: 4, py: 1.5 }} disabled={equipos.length === 0}>
                    Generar Formato de Entrada
                </Button>
            </Box>
        </Box>
    );
}