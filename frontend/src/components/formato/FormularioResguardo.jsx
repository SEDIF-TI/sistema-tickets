import { useContext } from 'react';
import {
    Box, Typography, Button, Paper, Grid,
    InputAdornment, Card, CardContent, Stack
} from '@mui/material';
import PersonIcon from '@mui/icons-material/Person';
import ComputerIcon from '@mui/icons-material/Computer';
import CommentIcon from '@mui/icons-material/Comment';
import BadgeIcon from '@mui/icons-material/Badge';
import PhoneIcon from '@mui/icons-material/Phone';
import { useForm, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import { resguardoService } from '../../services/resguardoService';
import { AuthContext } from '../../context/AuthContext.jsx';
import { useNotification } from '../../context/NotificationContext.jsx';
import { esquemaResguardo } from '../../util/esquemas';

import CampoFormulario from '../CampoFormulario';

/** Fondo apenas perceptible que separa la primera tarjeta de las demás. */
const COLOR_FONDO_SECUNDARIO = 'grey.50';

/** Estado del equipo en el momento de la entrega. */
const OPCIONES_CONDICIONES = [
    { valor: 'BUENO', etiqueta: 'BUENO' },
    { valor: 'REGULAR', etiqueta: 'REGULAR' },
    { valor: 'MALO', etiqueta: 'MALO' },
];

/**
 * Unidades de vigencia que acepta `ResguardoRequest`.
 *
 * En pantalla la opción sin plazo se lee «permanente», que es como la llama el
 * área, pero al backend viaja como `indefinido`, que es el valor del contrato.
 */
const OPCIONES_VIGENCIA = [
    { valor: 'indefinido', etiqueta: 'PERMANENTE (HASTA BAJA LABORAL)' },
    { valor: 'dias', etiqueta: 'DÍAS HÁBILES' },
    { valor: 'semanas', etiqueta: 'SEMANAS' },
    { valor: 'meses', etiqueta: 'MESES' },
];

const VALORES_INICIALES = {
    solicitanteNombre: '',
    solicitanteNumero: '',
    telefono: '',
    departamento: '',
    equipoNombre: '',
    numeroSerie: '',
    numeroInventario: '',
    condiciones: 'BUENO',
    accesorios: '',
    duracionTipo: 'indefinido',
    duracionCantidad: '30',
};

/**
 * Encabezado de sección del formulario.
 *
 * Vive fuera del componente a propósito: definido dentro, React lo trata como
 * un tipo distinto en cada render, desmonta el subárbol y los campos pierden
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

/**
 * Alta de un resguardo con emisión de la responsiva.
 *
 * El formulario lo gobierna React Hook Form con `esquemaResguardo`, que espeja
 * las restricciones de `ResguardoRequest`: los errores se señalan en el campo
 * correspondiente en lugar de descubrirse al enviar.
 *
 * El resguardo se registra primero y solo entonces se genera el documento:
 * emitir una responsiva de algo que no llegó a guardarse deja un papel firmado
 * sin respaldo en el sistema.
 */
export default function FormularioResguardo({ solicitarPdf, generando = false }) {
    const { user } = useContext(AuthContext);
    const { notificar, notificarError } = useNotification();

    const {
        control,
        handleSubmit,
        formState: { isSubmitting },
    } = useForm({
        resolver: zodResolver(esquemaResguardo),
        mode: 'onBlur',
        defaultValues: { ...VALORES_INICIALES, solicitanteNombre: user?.nombre || '' },
    });

    // Con vigencia permanente la cantidad sobra: se oculta en lugar de dejar un
    // campo que no influye en nada.
    const duracionTipo = useWatch({ control, name: 'duracionTipo' });
    const exigePlazo = duracionTipo !== 'indefinido';

    const guardar = async (datos) => {
        // Sin plazo la cantidad se omite en lugar de enviarse como 0: el DTO
        // exige @Min(1) y rechazaría el resguardo.
        const carga = {
            ...datos,
            duracionCantidad: exigePlazo ? Number(datos.duracionCantidad) : undefined,
        };

        try {
            await resguardoService.create(carga);
            await solicitarPdf('resguardos', carga, `Resguardo de ${carga.solicitanteNombre}`);
            notificar('Resguardo registrado y documento generado.');
        } catch (error) {
            // El error se propaga tal cual: el backend indica qué campo falló y
            // notificarError lo toma de `error.mensaje`.
            notificarError(error);
        }
    };

    return (
        <Box sx={{ p: { xs: 1, md: 3 }, maxWidth: '1200px', margin: '0 auto' }}>
            <Paper variant="outlined" sx={{ p: 4, borderRadius: 4, bgcolor: 'background.paper' }}>

                <Box sx={{ mb: 4, textAlign: 'center' }}>
                    <Typography variant="h4" sx={{ fontWeight: 900, color: 'primary.main', mb: 1 }}>
                        Responsiva de Resguardo de Equipo
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        Ingrese las especificaciones correspondientes. El sistema procesará el formato de firmas oficial de manera automática.
                    </Typography>
                </Box>

                <form onSubmit={handleSubmit(guardar)} noValidate>
                    <Grid container spacing={4}>

                        <Grid size={{ xs: 12 }}>
                            <Card variant="outlined" sx={{ bgcolor: COLOR_FONDO_SECUNDARIO, borderRadius: 3, borderStyle: 'dashed' }}>
                                <CardContent sx={{ p: 3 }}>
                                    <SectionHeader icon={PersonIcon} title="I. Identificación del Servidor Público / Solicitante" />
                                    <Grid container spacing={2}>
                                        {/* Los datos son del servidor público que recibe el
                                            equipo, no de quien rellena el formulario: los
                                            captura soporte a mano. */}
                                        <Grid size={{ xs: 12, md: 6 }}>
                                            <CampoFormulario
                                                control={control}
                                                nombre="solicitanteNombre"
                                                etiqueta="Nombre Completo"
                                                obligatorio
                                                mayusculas
                                            />
                                        </Grid>
                                        <Grid size={{ xs: 12, md: 3 }}>
                                            {/* Los adornos del campo van en `slotProps.input`,
                                                que es la vía de MUI 9 para llegar al
                                                componente interno del input. */}
                                            <CampoFormulario
                                                control={control}
                                                nombre="solicitanteNumero"
                                                etiqueta="No. Empleado"
                                                mayusculas
                                                slotProps={{
                                                    input: {
                                                        startAdornment: (
                                                            <InputAdornment position="start">
                                                                <BadgeIcon fontSize="small" />
                                                            </InputAdornment>
                                                        ),
                                                    },
                                                }}
                                            />
                                        </Grid>
                                        <Grid size={{ xs: 12, md: 3 }}>
                                            <CampoFormulario
                                                control={control}
                                                nombre="telefono"
                                                etiqueta="Celular"
                                                slotProps={{
                                                    input: {
                                                        startAdornment: (
                                                            <InputAdornment position="start">
                                                                <PhoneIcon fontSize="small" />
                                                            </InputAdornment>
                                                        ),
                                                    },
                                                }}
                                            />
                                        </Grid>
                                        <Grid size={{ xs: 12 }}>
                                            <CampoFormulario
                                                control={control}
                                                nombre="departamento"
                                                etiqueta="Departamento o Área de Adscripción"
                                                mayusculas
                                            />
                                        </Grid>
                                    </Grid>
                                </CardContent>
                            </Card>
                        </Grid>

                        <Grid size={{ xs: 12 }}>
                            <Card variant="outlined" sx={{ borderRadius: 3, borderStyle: 'dashed' }}>
                                <CardContent sx={{ p: 3 }}>
                                    <SectionHeader icon={ComputerIcon} title="II. Especificaciones del Bien Informático y Temporalidad" />
                                    <Grid container spacing={2}>
                                        <Grid size={{ xs: 12, md: 6 }}>
                                            <CampoFormulario
                                                control={control}
                                                nombre="equipoNombre"
                                                etiqueta="Descripción del Equipo"
                                                obligatorio
                                                mayusculas
                                                placeholder="Ej. LAPTOP DELL LATITUDE"
                                            />
                                        </Grid>
                                        <Grid size={{ xs: 12, md: 3 }}>
                                            <CampoFormulario
                                                control={control}
                                                nombre="numeroSerie"
                                                etiqueta="Número de Serie"
                                                obligatorio
                                                mayusculas
                                            />
                                        </Grid>
                                        <Grid size={{ xs: 12, md: 3 }}>
                                            <CampoFormulario
                                                control={control}
                                                nombre="numeroInventario"
                                                etiqueta="Número de Inventario"
                                                mayusculas
                                            />
                                        </Grid>
                                        <Grid size={{ xs: 12, md: 4 }}>
                                            <CampoFormulario
                                                control={control}
                                                nombre="condiciones"
                                                etiqueta="Estado Físico Actual"
                                                opciones={OPCIONES_CONDICIONES}
                                            />
                                        </Grid>
                                        <Grid size={{ xs: 12, md: exigePlazo ? 4 : 8 }}>
                                            <CampoFormulario
                                                control={control}
                                                nombre="duracionTipo"
                                                etiqueta="Tipo de Vigencia"
                                                opciones={OPCIONES_VIGENCIA}
                                            />
                                        </Grid>
                                        {exigePlazo && (
                                            <Grid size={{ xs: 12, md: 4 }}>
                                                <CampoFormulario
                                                    control={control}
                                                    nombre="duracionCantidad"
                                                    etiqueta={`Cantidad de ${duracionTipo}`}
                                                    type="number"
                                                    obligatorio
                                                />
                                            </Grid>
                                        )}
                                    </Grid>
                                </CardContent>
                            </Card>
                        </Grid>

                        <Grid size={{ xs: 12 }}>
                            <Card variant="outlined" sx={{ borderRadius: 3, borderStyle: 'dashed' }}>
                                <CardContent sx={{ p: 3 }}>
                                    <SectionHeader icon={CommentIcon} title="III. Inventario de Componentes y Notas" />
                                    <Grid container spacing={2}>
                                        <Grid size={{ xs: 12 }}>
                                            <CampoFormulario
                                                control={control}
                                                nombre="accesorios"
                                                etiqueta="Accesorios periféricos incluidos"
                                                maximo={500}
                                                mayusculas
                                                multiline
                                                rows={3}
                                                placeholder="Ej. Cargador, mouse, mochila, candado..."
                                            />
                                        </Grid>
                                    </Grid>
                                </CardContent>
                            </Card>
                        </Grid>
                    </Grid>

                    <Box sx={{ mt: 5, display: 'flex', justifyContent: 'center' }}>
                        <Button
                            type="submit"
                            disabled={generando || isSubmitting}
                            variant="contained"
                            size="large"
                            sx={{
                                '&:hover': { transform: 'scale(1.02)' },
                                transition: 'all 0.2s',
                                px: 10,
                                py: 2,
                                fontWeight: 'bold',
                                borderRadius: '50px',
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
