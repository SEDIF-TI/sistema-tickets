import React, { useState } from 'react';
import { 
    Typography, Paper, TextField, Button, Grid, Box, 
    Dialog, DialogTitle, DialogContent, DialogActions, MenuItem,
    Snackbar, Alert // <-- Importamos los componentes flotantes
} from '@mui/material';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import TableViewIcon from '@mui/icons-material/TableView';
import AddTaskIcon from '@mui/icons-material/AddTask';
import api from '../../services/api';

const COLOR_GUINDA = '#801A36';

export default function ReporteActividadesFormato({ solicitarPdf }) {
    const [fechas, setFechas] = useState({ fechaInicio: '', fechaFin: '' });
    const [modalAbierto, setModalAbierto] = useState(false);
    
    // NUEVO: Estado para controlar el mensaje flotante sin romper el diseño
    const [notificacion, setNotificacion] = useState({ abierto: false, mensaje: '', tipo: 'success' });

    const [actividad, setActividad] = useState({
        planTrabajoClave: '',
        actividadSolicitada: '',
        situacionActual: '',
        justificacion: ''
    });

    const usuarioLogueado = JSON.parse(localStorage.getItem('user')) || {};
    const idRealUsuario = usuarioLogueado.usuarioId; 

    // Función para cerrar la notificación flotante
    const handleCloseNotificacion = () => {
        setNotificacion({ ...notificacion, abierto: false });
    };

    const validarFechas = () => {
        if (!fechas.fechaInicio || !fechas.fechaFin) {
            setNotificacion({ abierto: true, mensaje: "Por favor selecciona ambas fechas.", tipo: "warning" });
            return false;
        }
        if (fechas.fechaInicio > fechas.fechaFin) {
            setNotificacion({ abierto: true, mensaje: "La fecha de inicio no puede ser mayor a la final.", tipo: "error" });
            return false;
        }
        return true;
    };

    const handleGenerarPdf = async () => {
        if (!validarFechas()) return;
        
        try {
            // 1. Empaquetamos los datos (con un seguro extra por si cambia el nombre del ID)
            const idSeguro = usuarioLogueado.usuarioId || usuarioLogueado.id || usuarioLogueado.pn_id || 0;
            const payloadDocumento = {
                ...fechas,
                usuarioId: String(idSeguro),
                rol: usuarioLogueado.rol || ''
            };

            // 2. HACEMOS LA PETICIÓN DIRECTA (Bypass a la función solicitarPdf del padre)
            const response = await api.post('/v1/documentos/reporte-actividades', payloadDocumento, { responseType: 'blob' });
            
            // 3. Forzamos la descarga del archivo en el navegador
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `Reporte_Actividades_${fechas.fechaInicio}_al_${fechas.fechaFin}.pdf`);
            document.body.appendChild(link);
            link.click();
            link.remove();
            
        } catch (error) {
            console.error("Error al generar PDF:", error);
            setNotificacion({ abierto: true, mensaje: "Error al descargar el PDF.", tipo: "error" });
        }
    };

    const handleGenerarExcel = async () => {
        if (!validarFechas()) return;
        try {
            // EMPAQUETAMOS LAS FECHAS + LOS DATOS DE SESIÓN
            const payloadDocumento = {
                ...fechas,
                usuarioId: String(idRealUsuario),
                rol: usuarioLogueado.rol || ''
            };

            const response = await api.post('/v1/documentos/reporte-actividades/excel', payloadDocumento, { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `Reporte_Actividades_${fechas.fechaInicio}_al_${fechas.fechaFin}.xlsx`);
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch (error) {
            console.error("Error al generar Excel:", error);
            setNotificacion({ abierto: true, mensaje: "Error al descargar el Excel.", tipo: "error" });
        }
    };

    const handleGuardarActividad = async () => {
        if (!actividad.planTrabajoClave || !actividad.actividadSolicitada || !actividad.situacionActual || !actividad.justificacion) {
            setNotificacion({ abierto: true, mensaje: "Llena todos los campos obligatorios.", tipo: "warning" });
            return;
        }
        
        if (!idRealUsuario) {
            setNotificacion({ abierto: true, mensaje: "Error de sesión. Vuelve a entrar.", tipo: "error" });
            return;
        }

        try {
            await api.post('/v1/actividades-extras', {
                ...actividad,
                usuarioId: String(idRealUsuario)
            });
            
            // ÉXITO: Mostramos el mensaje flotante, cerramos el modal y limpiamos
            setNotificacion({ abierto: true, mensaje: "¡Actividad guardada correctamente!", tipo: "success" });
            setModalAbierto(false);
            setActividad({ planTrabajoClave: '', actividadSolicitada: '', situacionActual: '', justificacion: '' });
        } catch (error) {
            console.error("Error al guardar actividad:", error);
            setNotificacion({ abierto: true, mensaje: "No se pudo guardar la actividad.", tipo: "error" });
        }
    };

    const handleChangeActividad = (e) => {
        setActividad({ ...actividad, [e.target.name]: e.target.value.toUpperCase() });
    };

    return (
        <> {/* <-- Usamos un Fragmento para separar el diseño de la notificación */}
            <Paper sx={{ p: 4, borderRadius: 2, boxShadow: 2, textAlign: 'center' }}>
                <Typography variant="h6" sx={{ color: COLOR_GUINDA, fontWeight: 'bold', mb: 2 }}>
                    Generar Reporte de Actividades (Consolidado)
                </Typography>
                <Typography variant="body2" color="textSecondary" sx={{ mb: 4 }}>
                    Selecciona el rango de fechas para descargar los tickets cerrados y las actividades administrativas/desarrollo.
                </Typography>

                <Box sx={{ mb: 4 }}>
                    <Button 
                        variant="outlined" 
                        onClick={() => setModalAbierto(true)} 
                        startIcon={<AddTaskIcon />}
                        sx={{ color: COLOR_GUINDA, borderColor: COLOR_GUINDA, '&:hover': { bgcolor: '#fce4ec', borderColor: COLOR_GUINDA } }}
                    >
                        + Registrar Actividad Manual
                    </Button>
                </Box>

                {/* --- CORRECCIÓN DEFINITIVA DE LAS FECHAS --- */}
                {/* Separamos la etiqueta visualmente para que JAMÁS se encime con el dd/mm/aaaa */}
                <Grid container spacing={3} justifyContent="center" sx={{ mb: 4 }}>
                    <Grid item xs={12} sm={4}>
                        <Typography variant="caption" color="textSecondary" sx={{ display: 'block', textAlign: 'left', mb: 0.5, ml: 1, fontWeight: 'bold' }}>
                            Fecha de Inicio
                        </Typography>
                        <TextField 
                            fullWidth 
                            type="date" 
                            value={fechas.fechaInicio} 
                            onChange={(e) => setFechas({...fechas, fechaInicio: e.target.value})} 
                        />
                    </Grid>
                    <Grid item xs={12} sm={4}>
                        <Typography variant="caption" color="textSecondary" sx={{ display: 'block', textAlign: 'left', mb: 0.5, ml: 1, fontWeight: 'bold' }}>
                            Fecha Fin
                        </Typography>
                        <TextField 
                            fullWidth 
                            type="date" 
                            value={fechas.fechaFin} 
                            onChange={(e) => setFechas({...fechas, fechaFin: e.target.value})} 
                        />
                    </Grid>
                </Grid>

                <Box sx={{ borderTop: '1px solid #eee', pt: 3, display: 'flex', justifyContent: 'center', gap: 2 }}>
                    <Button variant="contained" onClick={handleGenerarPdf} startIcon={<PictureAsPdfIcon />} 
                        sx={{ bgcolor: COLOR_GUINDA, '&:hover': { bgcolor: '#5e1227' } }}>
                        Descargar PDF
                    </Button>
                    
                    <Button variant="contained" onClick={handleGenerarExcel} startIcon={<TableViewIcon />} 
                        sx={{ bgcolor: '#1D6F42', '&:hover': { bgcolor: '#155331' } }}>
                        Descargar Excel
                    </Button>
                </Box>

                {/* MODAL DE REGISTRO */}
                <Dialog open={modalAbierto} onClose={() => setModalAbierto(false)} maxWidth="sm" fullWidth>
                    <DialogTitle sx={{ bgcolor: COLOR_GUINDA, color: 'white' }}>Registrar Actividad Manual</DialogTitle>
                    <DialogContent sx={{ mt: 2 }}>
                        <TextField
                            select fullWidth label="Clave del Plan de Trabajo *" name="planTrabajoClave"
                            value={actividad.planTrabajoClave} onChange={handleChangeActividad}
                            variant="outlined" margin="dense" sx={{ mb: 2 }}
                        >
                            <MenuItem value="1">1 - Mantenimiento preventivo equipo oficinas centrales</MenuItem>
                            <MenuItem value="5">5 - Mantenimiento de Sistemas Institucionales</MenuItem>
                            <MenuItem value="6">6 - Mantenimiento Preventivo Servidores</MenuItem>
                            <MenuItem value="7">7 - Soporte técnico a equipo de cómputo</MenuItem>
                            <MenuItem value="11">11 - Realización de respaldos de BD</MenuItem>
                        </TextField>

                        <TextField
                            fullWidth label="Actividad Solicitada (Ej. Desarrollo de Módulo, Reunión) *" name="actividadSolicitada"
                            value={actividad.actividadSolicitada} onChange={handleChangeActividad}
                            variant="outlined" margin="dense" sx={{ mb: 2 }}
                        />

                        <TextField
                            fullWidth label="Situación Actual (Ej. EN PROCESO, COMPLETADO) *" name="situacionActual"
                            value={actividad.situacionActual} onChange={handleChangeActividad}
                            variant="outlined" margin="dense" sx={{ mb: 2 }}
                        />

                        <TextField
                            fullWidth multiline rows={3} label="Actividad de Solución / Avance *" name="justificacion"
                            value={actividad.justificacion} onChange={handleChangeActividad}
                            variant="outlined" margin="dense" sx={{ mb: 2 }}
                        />
                    </DialogContent>
                    <DialogActions sx={{ p: 2, pt: 0 }}>
                        <Button onClick={() => setModalAbierto(false)} color="inherit">Cancelar</Button>
                        <Button variant="contained" onClick={handleGuardarActividad} sx={{ bgcolor: COLOR_GUINDA }}>Guardar Actividad</Button>
                    </DialogActions>
                </Dialog>
            </Paper>

            {/* --- CORRECCIÓN DEFINITIVA DE LA NOTIFICACIÓN --- */}
            {/* El Snackbar ahora vive fuera del Paper, por lo que flotará sobre la pantalla sin mover nada */}
            <Snackbar 
                open={notificacion.abierto} 
                autoHideDuration={4000} 
                onClose={handleCloseNotificacion}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
            >
                <Alert onClose={handleCloseNotificacion} severity={notificacion.tipo} sx={{ width: '100%' }}>
                    {notificacion.mensaje}
                </Alert>
            </Snackbar>
        </>
    );
}