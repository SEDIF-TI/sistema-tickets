import { useState } from 'react';
import { 
    Typography, Paper, TextField, Button, Grid, Box, 
    Dialog, DialogTitle, DialogContent, DialogActions, MenuItem,
} from '@mui/material';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import TableViewIcon from '@mui/icons-material/TableView';
import AddTaskIcon from '@mui/icons-material/AddTask';
import api from '../../services/api';
import { useNotification } from '../../context/NotificationContext.jsx';


export default function ReporteActividadesFormato({ solicitarPdf, generando = false }) {
    const { notificar, notificarError, notificarAdvertencia } = useNotification();
    const [fechas, setFechas] = useState({ fechaInicio: '', fechaFin: '' });
    const [modalAbierto, setModalAbierto] = useState(false);

    const [actividad, setActividad] = useState({
        planTrabajoClave: '',
        actividadSolicitada: '',
        situacionActual: '',
        justificacion: ''
    });

    const usuarioLogueado = JSON.parse(localStorage.getItem('user')) || {};
    const idRealUsuario = usuarioLogueado.usuarioId || usuarioLogueado.id || usuarioLogueado.pn_id;

    const validarFechas = () => {
        if (!fechas.fechaInicio || !fechas.fechaFin) {
            notificarAdvertencia('Selecciona las dos fechas del periodo.');
            return false;
        }
        if (fechas.fechaInicio > fechas.fechaFin) {
            notificarAdvertencia('La fecha de inicio no puede ser posterior a la final.');
            return false;
        }
        return true;
    };

    /**
     * Periodo y alcance que espera `ReporteActividadesRequest`. El rol decide
     * si el reporte abarca la institución o solo los tickets del usuario.
     */
    const construirPayload = () => ({
        ...fechas,
        usuarioId: Number(idRealUsuario) || 0,
        rol: usuarioLogueado.rol || '',
    });

    const handleGenerarPdf = async () => {
        if (!validarFechas()) return;

        await solicitarPdf('reporte-actividades', construirPayload(),
            `Reporte_Actividades_${fechas.fechaInicio}.pdf`);
    };

    const handleGenerarExcel = async () => {
        if (!validarFechas()) return;

        try {
             const response = await api.post('/v1/documentos/reporte-actividades/excel', construirPayload(), { responseType: 'blob' });
             const url = window.URL.createObjectURL(new Blob([response.data]));
             const link = document.createElement('a');
             link.href = url;
             link.setAttribute('download', `Reporte_Actividades_${fechas.fechaInicio}.xlsx`);
             document.body.appendChild(link);
             link.click();
             link.remove();
        } catch (error) {
            notificarError(error);
        }
    };

    const handleGuardarActividad = async () => {
        if (!actividad.planTrabajoClave || !actividad.actividadSolicitada || !actividad.situacionActual || !actividad.justificacion) {
            notificarAdvertencia('Completa los campos obligatorios de la actividad.');
            return;
        }
        
        if (!idRealUsuario) {
            notificarError('No se pudo identificar tu sesión. Cierra sesión y vuelve a entrar.');
            return;
        }

        try {
            await api.post('/v1/actividades-extras', {
                ...actividad,
                usuarioId: String(idRealUsuario)
            });
            
            notificar('¡Actividad guardada correctamente!');
            setModalAbierto(false);
            setActividad({ planTrabajoClave: '', actividadSolicitada: '', situacionActual: '', justificacion: '' });
        } catch (error) {
            notificarError(error);
        }
    };

    const handleChangeActividad = (e) => {
        setActividad({ ...actividad, [e.target.name]: e.target.value.toUpperCase() });
    };

    return (
        // ... (El JSX se mantiene exactamente igual al que ya tenías)
        <>
            <Paper sx={{ p: { xs: 3, md: 4 }, borderRadius: 2, boxShadow: 2, width: '100%', boxSizing: 'border-box' }}>
                <Box sx={{ display: 'flex', flexDirection: { xs: 'column', md: 'row' }, justifyContent: 'space-between', alignItems: { xs: 'flex-start', md: 'center' }, mb: 4, gap: 2 }}>
                    <Box>
                        <Typography variant="h6" sx={{ color: 'primary.main', fontWeight: 'bold' }}>
                            Reporte de Actividades (Consolidado)
                        </Typography>
                        <Typography variant="body2" color="textSecondary">
                            Consulta y exporta el historial de tickets cerrados y tareas manuales realizadas en un periodo.
                        </Typography>
                    </Box>
                    <Button 
                        variant="outlined" 
                        onClick={() => setModalAbierto(true)} 
                        startIcon={<AddTaskIcon />}
                        sx={{ color: 'primary.main', borderColor: 'primary.main', '&:hover': { bgcolor: '#fce4ec', borderColor: 'primary.main' }, fontWeight: 'bold', minWidth: 'max-content' }}
                    >
                        Registrar Actividad Manual
                    </Button>
                </Box>

                <Paper elevation={0} sx={{ bgcolor: '#f8fafc', p: 3, borderRadius: 2, border: '1px solid #e2e8f0' }}>
                    <Grid container spacing={3} alignItems="flex-end">
                        <Grid size={{ xs: 12, md: 3 }}>
                            <Typography variant="caption" color="textSecondary" sx={{ display: 'block', mb: 1, fontWeight: 'bold', ml: 0.5 }}>Fecha de Inicio</Typography>
                            <TextField fullWidth size="small" type="date" value={fechas.fechaInicio} onChange={(e) => setFechas({...fechas, fechaInicio: e.target.value})} sx={{ bgcolor: 'white' }} />
                        </Grid>
                        <Grid size={{ xs: 12, md: 3 }}>
                            <Typography variant="caption" color="textSecondary" sx={{ display: 'block', mb: 1, fontWeight: 'bold', ml: 0.5 }}>Fecha Fin</Typography>
                            <TextField fullWidth size="small" type="date" value={fechas.fechaFin} onChange={(e) => setFechas({...fechas, fechaFin: e.target.value})} sx={{ bgcolor: 'white' }} />
                        </Grid>
                        <Grid size={{ xs: 12, md: 6 }} sx={{ display: 'flex', justifyContent: { xs: 'flex-start', md: 'flex-end' }, gap: 2 }}>
                            <Button variant="contained" disabled={generando} onClick={handleGenerarPdf} startIcon={<PictureAsPdfIcon />} sx={{ bgcolor: 'primary.main', '&:hover': { bgcolor: '#5e1227' }, flexGrow: { xs: 1, md: 0 }, px: 3 }}>Descargar PDF</Button>
                            <Button variant="contained" disabled={generando} onClick={handleGenerarExcel} startIcon={<TableViewIcon />} sx={{ bgcolor: '#1D6F42', '&:hover': { bgcolor: '#155331' }, flexGrow: { xs: 1, md: 0 }, px: 3 }}>Descargar Excel</Button>
                        </Grid>
                    </Grid>
                </Paper>
                {/* ... (El resto del modal sigue igual) */}
                <Dialog open={modalAbierto} onClose={() => setModalAbierto(false)} maxWidth="sm" fullWidth>
                    <DialogTitle sx={{ bgcolor: 'primary.main', color: 'white' }}>Registrar Actividad Manual</DialogTitle>
                    <DialogContent sx={{ mt: 2 }}>
                        <TextField select fullWidth label="Clave del Plan de Trabajo *" name="planTrabajoClave" value={actividad.planTrabajoClave} onChange={handleChangeActividad} variant="outlined" margin="dense" sx={{ mb: 2 }}>
                            <MenuItem value="1">1 - Mantenimiento preventivo equipo oficinas centrales</MenuItem>
                            <MenuItem value="5">5 - Mantenimiento de Sistemas Institucionales</MenuItem>
                            <MenuItem value="6">6 - Mantenimiento Preventivo Servidores</MenuItem>
                            <MenuItem value="7">7 - Soporte técnico a equipo de cómputo</MenuItem>
                            <MenuItem value="11">11 - Realización de respaldos de BD</MenuItem>
                        </TextField>
                        <TextField fullWidth label="Actividad Solicitada *" name="actividadSolicitada" value={actividad.actividadSolicitada} onChange={handleChangeActividad} variant="outlined" margin="dense" sx={{ mb: 2 }} />
                        <TextField fullWidth label="Situación Actual *" name="situacionActual" value={actividad.situacionActual} onChange={handleChangeActividad} variant="outlined" margin="dense" sx={{ mb: 2 }} />
                        <TextField fullWidth multiline rows={3} label="Actividad de Solución *" name="justificacion" value={actividad.justificacion} onChange={handleChangeActividad} variant="outlined" margin="dense" sx={{ mb: 2 }} />
                    </DialogContent>
                    <DialogActions sx={{ p: 2, pt: 0 }}>
                        <Button onClick={() => setModalAbierto(false)} color="inherit">Cancelar</Button>
                        <Button variant="contained" onClick={handleGuardarActividad} sx={{ bgcolor: 'primary.main' }}>Guardar Actividad</Button>
                    </DialogActions>
                </Dialog>
            </Paper>
        </>
    );
}