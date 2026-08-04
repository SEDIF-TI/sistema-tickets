import { useState, useContext } from 'react';
import { 
    Box, Typography, TextField, Button, Paper, MenuItem, Grid, 
    InputAdornment, Card, CardContent, Stack 
} from '@mui/material';
import PersonIcon from '@mui/icons-material/Person';
import ComputerIcon from '@mui/icons-material/Computer';
import CommentIcon from '@mui/icons-material/Comment';
import BadgeIcon from '@mui/icons-material/Badge';
import PhoneIcon from '@mui/icons-material/Phone';
import { resguardoService } from '../../services/resguardoService';
import { AuthContext } from '../../context/AuthContext.jsx';
import { useNotification } from '../../context/NotificationContext.jsx';
import { toUpper } from '../../util/formater';

const COLOR_FONDO_SECUNDARIO = '#fcfcfc';

/**
 * Encabezado de seccion del formulario.
 *
 * Vive fuera del componente a proposito: definido dentro, React lo trata como
 * un tipo distinto en cada render, desmonta el subarbol y los campos pierden
 * el foco mientras se escribe.
 */
const SectionHeader = ({ icon: Icon, title }) => (
    <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 2, borderLeft: '4px solid', borderColor: 'primary.main', pl: 1.5 }}>
        <Icon sx={{ color: 'primary.main', fontSize: 22 }} aria-hidden="true" />
        <Typography variant="subtitle1" sx={{ fontWeight: 700, letterSpacing: 0.5, textTransform: 'uppercase', fontSize: '0.85rem' }}>
            {title}
        </Typography>
    </Stack>
);

export default function FormularioResguardo({ solicitarPdf, generando = false }) {
    const { user } = useContext(AuthContext);
    const { notificar, notificarError } = useNotification();

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
            // El resguardo se registra primero y solo entonces se genera el
            // documento: emitir una responsiva de algo que no llego a guardarse
            // deja un papel firmado sin respaldo en el sistema.
            await resguardoService.create(payloadFinal);
            await solicitarPdf('resguardos', payloadFinal,
                `Resguardo de ${payloadFinal.solicitanteNombre}`);

            notificar('Resguardo registrado y documento generado.');
        } catch (error) {
            // El detalle real lo trae el backend: antes se sustituia por un
            // texto generico que no decia que campo estaba mal.
            notificarError(error);
        }
    };

    return (
        <Box sx={{ p: { xs: 1, md: 3 }, maxWidth: '1200px', margin: '0 auto' }}>
            <Paper elevation={0} sx={{ p: 4, borderRadius: 4, border: '1px solid #e0e0e0', bgcolor: '#fff' }}>
                
                <Box sx={{ mb: 4, textAlign: 'center' }}>
                    <Typography variant="h4" sx={{ fontWeight: 900, color: 'primary.main', mb: 1 }}>
                        Responsiva de Resguardo de Equipo
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        Ingrese las especificaciones correspondientes. El sistema procesará el formato de firmas oficial de manera automática.
                    </Typography>
                </Box>

                <form onSubmit={handleSubmit}>
                    <Grid container spacing={4}>
                        
                        {/* SECCIÓN I: SOLICITANTE */}
                        <Grid size={{ xs: 12 }}>
                            <Card variant="outlined" sx={{ bgcolor: COLOR_FONDO_SECUNDARIO, borderRadius: 3, borderStyle: 'dashed' }}>
                                <CardContent sx={{ p: 3 }}>
                                    <SectionHeader icon={PersonIcon} title="I. Identificación del Servidor Público / Solicitante" />
                                    <Grid container spacing={2}>
                                        <Grid size={{ xs: 12, md: 6 }}>
                                            <TextField fullWidth label="Nombre Completo" name="solicitanteNombre" value={formData.solicitanteNombre} onChange={handleChange} required variant="filled" />
                                        </Grid>
                                        <Grid size={{ xs: 12, md: 3 }}>
                                            <TextField fullWidth label="No. Empleado" name="solicitanteNumero" value={formData.solicitanteNumero} onChange={handleChange} required variant="filled" InputProps={{ startAdornment: <InputAdornment position="start"><BadgeIcon fontSize="small"/></InputAdornment> }} />
                                        </Grid>
                                        <Grid size={{ xs: 12, md: 3 }}>
                                            <TextField fullWidth label="Teléfono / Extensión" name="telefono" value={formData.telefono} onChange={handleChange} variant="filled" InputProps={{ startAdornment: <InputAdornment position="start"><PhoneIcon fontSize="small"/></InputAdornment> }} />
                                        </Grid>
                                        <Grid size={{ xs: 12 }}>
                                            <TextField fullWidth label="Departamento o Área de Adscripción" name="departamento" value={formData.departamento} onChange={handleChange} required variant="filled" />
                                        </Grid>
                                    </Grid>
                                </CardContent>
                            </Card>
                        </Grid>

                        {/* SECCIÓN II: EQUIPO */}
                        <Grid size={{ xs: 12 }}>
                            <Card variant="outlined" sx={{ borderRadius: 3, borderStyle: 'dashed' }}>
                                <CardContent sx={{ p: 3 }}>
                                    <SectionHeader icon={ComputerIcon} title="II. Especificaciones del Bien Informático y Temporalidad" />
                                    <Grid container spacing={2}>
                                        <Grid size={{ xs: 12, md: 6 }}>
                                            <TextField fullWidth label="Descripción del Equipo" name="equipoNombre" value={formData.equipoNombre} onChange={handleChange} required placeholder="Ej. LAPTOP DELL LATITUDE" />
                                        </Grid>
                                        <Grid size={{ xs: 12, md: 3 }}>
                                            <TextField fullWidth label="Número de Serie" name="numeroSerie" value={formData.numeroSerie} onChange={handleChange} required />
                                        </Grid>
                                        <Grid size={{ xs: 12, md: 3 }}>
                                            <TextField fullWidth label="Número de Inventario" name="numeroInventario" value={formData.numeroInventario} onChange={handleChange} />
                                        </Grid>
                                        <Grid size={{ xs: 12, md: 4 }}>
                                            <TextField select fullWidth label="Estado Físico Actual" name="condiciones" value={formData.condiciones} onChange={handleChange}>
                                                <MenuItem value="BUENO">🟢 BUENO</MenuItem>
                                                <MenuItem value="REGULAR">🟡 REGULAR</MenuItem>
                                                <MenuItem value="MALO">🔴 MALO</MenuItem>
                                            </TextField>
                                        </Grid>
                                        <Grid size={{ xs: 12, md: formData.duracionTipo !== 'PERMANENTE' ? 4 : 8 }}>
                                            <TextField select fullWidth label="Tipo de Vigencia" name="duracionTipo" value={formData.duracionTipo} onChange={handleChange} sx={{ '& .MuiSelect-select': { fontWeight: 'bold' } }}>
                                                <MenuItem value="PERMANENTE">PERMANENTE (HASTA BAJA LABORAL)</MenuItem>
                                                <MenuItem value="DIAS">DÍAS HÁBILES</MenuItem>
                                                <MenuItem value="SEMANAS">SEMANAS</MenuItem>
                                            </TextField>
                                        </Grid>
                                        {formData.duracionTipo !== 'PERMANENTE' && (
                                            <Grid size={{ xs: 12, md: 4 }}>
                                                <TextField fullWidth type="number" name="duracionCantidad" label={`Cantidad de ${formData.duracionTipo === 'DIAS' ? 'días' : 'semanas'}`} value={formData.duracionCantidad} onChange={handleChange} inputProps={{ min: 1 }} required color="primary" focused />
                                            </Grid>
                                        )}
                                    </Grid>
                                </CardContent>
                            </Card>
                        </Grid>

                        {/* SECCIÓN III: COMPLEMENTOS */}
                        <Grid size={{ xs: 12 }}>
                            <Card variant="outlined" sx={{ borderRadius: 3, borderStyle: 'dashed' }}>
                                <CardContent sx={{ p: 3 }}>
                                    <SectionHeader icon={CommentIcon} title="III. Inventario de Componentes y Notas" />
                                    <Grid container spacing={2}>
                                        <Grid size={{ xs: 12, md: 6 }}>
                                            <TextField fullWidth multiline rows={3} name="accesorios" label="Accesorios periféricos incluidos" value={formData.accesorios} onChange={handleChange} placeholder="Ej. Cargador, mouse, mochila, candado..." />
                                        </Grid>
                                        <Grid size={{ xs: 12, md: 6 }}>
                                            <TextField fullWidth multiline rows={3} name="observaciones" label="Observaciones o comentarios adicionales" value={formData.observaciones} onChange={handleChange} />
                                        </Grid>
                                    </Grid>
                                </CardContent>
                            </Card>
                        </Grid>
                    </Grid>

                    {/* BOTÓN DE ACCIÓN */}
                    <Box sx={{ mt: 5, display: 'flex', justifyContent: 'center' }}>
                        <Button disabled={generando} 
                            type="submit" disabled={generando} 
                            variant="contained" 
                            size="large"
                            sx={{ 
                                bgcolor: 'primary.main', 
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