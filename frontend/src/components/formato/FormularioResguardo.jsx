import { useState, useContext } from 'react';
import { 
    Box, Typography, TextField, Button, Alert, Paper, MenuItem, Grid, 
    InputAdornment, Card, CardContent, Stack 
} from '@mui/material';
import PersonIcon from '@mui/icons-material/Person';
import ComputerIcon from '@mui/icons-material/Computer';
import CommentIcon from '@mui/icons-material/Comment';
import BadgeIcon from '@mui/icons-material/Badge';
import PhoneIcon from '@mui/icons-material/Phone';
import api from '../../services/api';
import { AuthContext } from '../../context/AuthContext.jsx';
import { toUpper } from '../../util/formater';

const COLOR_GUINDA = '#5c0a28';
const COLOR_FONDO_SECUNDARIO = '#fcfcfc';

export default function FormularioResguardo({ solicitarPdf }) {
    const { user } = useContext(AuthContext);

    const [formData, setFormData] = useState({
        solicitanteNombre: user?.nombre || '',
        solicitanteNumero: '', 
        telefono: '',
        departamento: '',
        equipoNombre: '',
        numeroSerie: '',
        numeroInventario: '',
        condiciones: 'BUENO',
        duracionCantidad: 1,
        duracionTipo: 'PERMANENTE',
        accesorios: '',
        observaciones: ''
    });

    const [mensaje, setMensaje] = useState({ tipo: '', texto: '' });

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: toUpper(value) }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        const payloadFinal = {
            ...formData,
            duracionCantidad: formData.duracionTipo === 'PERMANENTE' ? 0 : Number(formData.duracionCantidad)
        };

        try {
            // 1. Guardar en Base de Datos
            await api.post('/v1/resguardos', payloadFinal);
            
            // 2. Generar y abrir PDF oficial
            await solicitarPdf('resguardos', payloadFinal, `Resguardo_${payloadFinal.solicitanteNombre}.pdf`);
            
            setMensaje({ tipo: 'success', texto: 'Resguardo guardado y documento oficial generado con éxito.' });
        } catch (error) {
            console.error("Error al procesar resguardo:", error);
            setMensaje({ tipo: 'error', texto: 'Hubo un error al guardar el resguardo. Por favor, verifica los datos.' });
        }
    };

    const SectionHeader = ({ icon: Icon, title }) => (
        <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 2, borderLeft: `4px solid ${COLOR_GUINDA}`, pl: 1.5 }}>
            <Icon sx={{ color: COLOR_GUINDA, fontSize: 22 }} />
            <Typography variant="subtitle1" sx={{ fontWeight: 800, color: '#444', letterSpacing: 0.5, textTransform: 'uppercase', fontSize: '0.85rem' }}>
                {title}
            </Typography>
        </Stack>
    );

    return (
        <Box sx={{ p: { xs: 1, md: 3 }, maxWidth: '1200px', margin: '0 auto' }}>
            <Paper elevation={0} sx={{ p: 4, borderRadius: 4, border: '1px solid #e0e0e0', bgcolor: '#fff' }}>
                
                <Box sx={{ mb: 4, textAlign: 'center' }}>
                    <Typography variant="h4" sx={{ fontWeight: 900, color: COLOR_GUINDA, mb: 1 }}>
                        Responsiva de Resguardo de Equipo
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        Ingrese las especificaciones correspondientes. El sistema procesará el formato de firmas oficial de manera automática.
                    </Typography>
                </Box>

                {mensaje.texto && <Alert severity={mensaje.tipo} sx={{ mb: 4, borderRadius: 2 }}>{mensaje.texto}</Alert>}

                <form onSubmit={handleSubmit}>
                    <Grid container spacing={4}>
                        
                        {/* SECCIÓN I: SOLICITANTE */}
                        <Grid item xs={12}>
                            <Card variant="outlined" sx={{ bgcolor: COLOR_FONDO_SECUNDARIO, borderRadius: 3, borderStyle: 'dashed' }}>
                                <CardContent sx={{ p: 3 }}>
                                    <SectionHeader icon={PersonIcon} title="I. Identificación del Servidor Público / Solicitante" />
                                    <Grid container spacing={2}>
                                        <Grid item xs={12} md={6}>
                                            <TextField fullWidth label="Nombre Completo" name="solicitanteNombre" value={formData.solicitanteNombre} onChange={handleChange} required variant="filled" />
                                        </Grid>
                                        <Grid item xs={12} md={3}>
                                            <TextField fullWidth label="No. Empleado" name="solicitanteNumero" value={formData.solicitanteNumero} onChange={handleChange} required variant="filled" InputProps={{ startAdornment: <InputAdornment position="start"><BadgeIcon fontSize="small"/></InputAdornment> }} />
                                        </Grid>
                                        <Grid item xs={12} md={3}>
                                            <TextField fullWidth label="Teléfono / Extensión" name="telefono" value={formData.telefono} onChange={handleChange} variant="filled" InputProps={{ startAdornment: <InputAdornment position="start"><PhoneIcon fontSize="small"/></InputAdornment> }} />
                                        </Grid>
                                        <Grid item xs={12}>
                                            <TextField fullWidth label="Departamento o Área de Adscripción" name="departamento" value={formData.departamento} onChange={handleChange} required variant="filled" />
                                        </Grid>
                                    </Grid>
                                </CardContent>
                            </Card>
                        </Grid>

                        {/* SECCIÓN II: EQUIPO */}
                        <Grid item xs={12}>
                            <Card variant="outlined" sx={{ borderRadius: 3, borderStyle: 'dashed' }}>
                                <CardContent sx={{ p: 3 }}>
                                    <SectionHeader icon={ComputerIcon} title="II. Especificaciones del Bien Informático y Temporalidad" />
                                    <Grid container spacing={2}>
                                        <Grid item xs={12} md={6}>
                                            <TextField fullWidth label="Descripción del Equipo" name="equipoNombre" value={formData.equipoNombre} onChange={handleChange} required placeholder="Ej. LAPTOP DELL LATITUDE" />
                                        </Grid>
                                        <Grid item xs={12} md={3}>
                                            <TextField fullWidth label="Número de Serie" name="numeroSerie" value={formData.numeroSerie} onChange={handleChange} required />
                                        </Grid>
                                        <Grid item xs={12} md={3}>
                                            <TextField fullWidth label="Número de Inventario" name="numeroInventario" value={formData.numeroInventario} onChange={handleChange} />
                                        </Grid>
                                        <Grid item xs={12} md={4}>
                                            <TextField select fullWidth label="Estado Físico Actual" name="condiciones" value={formData.condiciones} onChange={handleChange}>
                                                <MenuItem value="BUENO">🟢 BUENO</MenuItem>
                                                <MenuItem value="REGULAR">🟡 REGULAR</MenuItem>
                                                <MenuItem value="MALO">🔴 MALO</MenuItem>
                                            </TextField>
                                        </Grid>
                                        <Grid item xs={12} md={formData.duracionTipo !== 'PERMANENTE' ? 4 : 8}>
                                            <TextField select fullWidth label="Tipo de Vigencia" name="duracionTipo" value={formData.duracionTipo} onChange={handleChange} sx={{ '& .MuiSelect-select': { fontWeight: 'bold' } }}>
                                                <MenuItem value="PERMANENTE">PERMANENTE (HASTA BAJA LABORAL)</MenuItem>
                                                <MenuItem value="DIAS">DÍAS HÁBILES</MenuItem>
                                                <MenuItem value="SEMANAS">SEMANAS</MenuItem>
                                            </TextField>
                                        </Grid>
                                        {formData.duracionTipo !== 'PERMANENTE' && (
                                            <Grid item xs={12} md={4}>
                                                <TextField fullWidth type="number" name="duracionCantidad" label={`Cantidad de ${formData.duracionTipo === 'DIAS' ? 'días' : 'semanas'}`} value={formData.duracionCantidad} onChange={handleChange} inputProps={{ min: 1 }} required color="primary" focused />
                                            </Grid>
                                        )}
                                    </Grid>
                                </CardContent>
                            </Card>
                        </Grid>

                        {/* SECCIÓN III: COMPLEMENTOS */}
                        <Grid item xs={12}>
                            <Card variant="outlined" sx={{ borderRadius: 3, borderStyle: 'dashed' }}>
                                <CardContent sx={{ p: 3 }}>
                                    <SectionHeader icon={CommentIcon} title="III. Inventario de Componentes y Notas" />
                                    <Grid container spacing={2}>
                                        <Grid item xs={12} md={6}>
                                            <TextField fullWidth multiline rows={3} name="accesorios" label="Accesorios periféricos incluidos" value={formData.accesorios} onChange={handleChange} placeholder="Ej. Cargador, mouse, mochila, candado..." />
                                        </Grid>
                                        <Grid item xs={12} md={6}>
                                            <TextField fullWidth multiline rows={3} name="observaciones" label="Observaciones o comentarios adicionales" value={formData.observaciones} onChange={handleChange} />
                                        </Grid>
                                    </Grid>
                                </CardContent>
                            </Card>
                        </Grid>
                    </Grid>

                    {/* BOTÓN DE ACCIÓN */}
                    <Box sx={{ mt: 5, display: 'flex', justifyContent: 'center' }}>
                        <Button 
                            type="submit" 
                            variant="contained" 
                            size="large"
                            sx={{ 
                                bgcolor: COLOR_GUINDA, 
                                '&:hover': { bgcolor: '#42061c', transform: 'scale(1.02)' }, 
                                transition: 'all 0.2s',
                                px: 10, 
                                py: 2, 
                                fontWeight: 'bold',
                                borderRadius: '50px',
                                boxShadow: '0 8px 16px rgba(92, 10, 40, 0.2)'
                            }}
                        >
                            Generar Resguardo Oficial
                        </Button>
                    </Box>
                </form>
            </Paper>
        </Box>
    );
}