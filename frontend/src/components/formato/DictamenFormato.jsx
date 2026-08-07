import { useState, useEffect, useContext } from 'react';
import { Typography, Paper, TextField, Button, Grid, Box, Divider, Autocomplete } from '@mui/material';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import api from '../../services/api';
import { AuthContext } from '../../context/AuthContext.jsx';
import { esquemaDictamen } from '../../util/esquemas';
import { toUpper } from '../../util/formater';

import CampoFormulario from '../CampoFormulario';

/** Cargos fijos del formato oficial, por estructura del área. */
const FIRMA_REVISA = 'C. MARCO POLO OLIVARES GONZALEZ';
const FIRMA_RECIBE = 'DRA. CARMEN GONZÁLEZ SERDÁN';

const VALORES_INICIALES = {
    cve: 'OT',
    descripcionEquipo: '',
    marca: '',
    modelo: '',
    serie: '',
    noResguardo: '',
    nombreUsuario: '',
    telefonoUsuario: '',
    direccionUsuario: '',
    departamentoUsuario: '',
    tipoReporte: '',
    fallaReportada: '',
    diagnostico: '',
};

/**
 * Dictamen técnico de un equipo.
 *
 * El formulario lo gobierna React Hook Form con el esquema `esquemaDictamen`,
 * que espeja las restricciones de `DictamenRequest`: los errores se señalan en
 * el campo correspondiente en lugar de descubrirse al enviar.
 *
 * Al generar el documento, los valores se completan con lo que el formato
 * imprime pero no se captura —fecha, firmas y el tratamiento «C. »— y viajan a
 * `solicitarPdf('dictamen', …)`, que compone el PDF y registra el dictamen.
 *
 * La descripción del equipo se captura con autocompletado contra el catálogo:
 * elegir una sugerencia rellena marca y modelo, y al emitir el documento la
 * descripción escrita se da de alta, de modo que esté disponible la próxima vez.
 */
export default function DictamenFormato({ solicitarPdf, generando = false }) {
    const { user } = useContext(AuthContext);

    const {
        control,
        handleSubmit,
        setValue,
        formState: { isSubmitting },
    } = useForm({
        resolver: zodResolver(esquemaDictamen),
        mode: 'onBlur',
        defaultValues: VALORES_INICIALES,
    });

    // Sugerencias del catálogo y texto que las busca.
    const [opcionesEquipo, setOpcionesEquipo] = useState([]);
    const [busquedaEquipo, setBusquedaEquipo] = useState('');

    // La consulta al catálogo espera 300 ms desde la última tecla, y no arranca
    // hasta el segundo carácter: con uno solo la lista devuelta no acota nada.
    useEffect(() => {
        if (busquedaEquipo.length < 2) return;

        let cancelado = false;

        const temporizador = setTimeout(async () => {
            try {
                const res = await api.get(`/v1/equipos/buscar?q=${busquedaEquipo}`);
                if (!cancelado) setOpcionesEquipo(res.data ?? []);
            } catch {
                // El catálogo es una ayuda para escribir, no un requisito: si
                // la consulta falla se sigue capturando a mano.
                if (!cancelado) setOpcionesEquipo([]);
            }
        }, 300);

        return () => {
            cancelado = true;
            clearTimeout(temporizador);
        };
    }, [busquedaEquipo]);

    /** Antepone el tratamiento que exige el formato, sin duplicarlo. */
    const conTratamiento = (nombre) => (
        !nombre || nombre.startsWith('C. ') ? nombre : `C. ${nombre}`
    );

    const emitir = async (datos) => {
        // El equipo se registra en el catálogo antes de emitir el documento,
        // para tenerlo como sugerencia la próxima vez. Si falla, el dictamen se
        // genera igual: el catálogo es una comodidad, no un requisito.
        try {
            await api.post('/v1/equipos/upsert', {
                descripcion: datos.descripcionEquipo,
                marca: datos.marca,
                modelo: datos.modelo,
            });
        } catch {
            // Sin efecto sobre la emisión.
        }

        // Lo que el formato imprime pero no se captura: la fecha del día, las
        // firmas y el tratamiento. El tratamiento se aplica solo en la copia
        // que viaja al PDF, de modo que el formulario conserva el nombre limpio.
        await solicitarPdf('dictamen', {
            ...datos,
            folioTicket: '',
            fecha: new Date().toLocaleDateString('es-MX'),
            concepto: '',
            observacion: '',
            realizadoPor: conTratamiento(user?.nombre || 'Técnico de Soporte'),
            revisadoPor: FIRMA_REVISA,
            recibidoPor: FIRMA_RECIBE,
            nombreUsuario: conTratamiento(datos.nombreUsuario),
        }, 'Dictamen_Tecnico.pdf');
    };

    return (
        <Paper sx={{ p: 4, borderRadius: 2, boxShadow: 2 }}>
            <form onSubmit={handleSubmit(emitir)} noValidate>
                <Typography variant="h6" sx={{ color: 'primary.main', fontWeight: 'bold', mb: 2 }}>
                    1. Datos Generales y del Equipo
                </Typography>

                {/* Rejilla de 12 columnas: los tramos de cada campo reparten
                    esta sección en dos filas de anchos desiguales. */}
                <Box sx={{ display: 'grid', gridTemplateColumns: { xs: 'repeat(1, 1fr)', sm: 'repeat(12, 1fr)' }, gap: 2 }}>

                    <Box sx={{ gridColumn: { xs: 'span 1', sm: 'span 2' } }}>
                        <TextField fullWidth size="small" label="Folio Ticket" value="Autogenerado" disabled sx={{ bgcolor: 'grey.50' }} />
                    </Box>

                    <Box sx={{ gridColumn: { xs: 'span 1', sm: 'span 2' } }}>
                        <TextField fullWidth size="small" label="Fecha" value={new Date().toLocaleDateString('es-MX')} disabled sx={{ bgcolor: 'grey.50' }} />
                    </Box>

                    <Box sx={{ gridColumn: { xs: 'span 1', sm: 'span 2' } }}>
                        <CampoFormulario control={control} nombre="cve" etiqueta="CVE (Ej: OT)" size="small" mayusculas />
                    </Box>

                    {/* La descripción ocupa media rejilla: es el campo más largo
                        y además despliega las sugerencias del catálogo. */}
                    <Box sx={{ gridColumn: { xs: 'span 1', sm: 'span 6' } }}>
                        <Controller
                            name="descripcionEquipo"
                            control={control}
                            render={({ field, fieldState }) => (
                                <Autocomplete
                                    freeSolo
                                    options={opcionesEquipo}
                                    getOptionLabel={(opcion) => (
                                        typeof opcion === 'string' ? opcion : opcion.descripcion
                                    )}
                                    inputValue={field.value ?? ''}
                                    onInputChange={(_evento, texto) => {
                                        setBusquedaEquipo(texto);
                                        field.onChange(toUpper(texto));
                                    }}
                                    onChange={(_evento, seleccion) => {
                                        // Elegir del catálogo arrastra marca y
                                        // modelo; escribir libre solo el texto.
                                        if (seleccion && typeof seleccion === 'object') {
                                            field.onChange(seleccion.descripcion || '');
                                            setValue('marca', seleccion.marca || '');
                                            setValue('modelo', seleccion.modelo || '');
                                        } else if (typeof seleccion === 'string') {
                                            field.onChange(toUpper(seleccion));
                                        }
                                    }}
                                    renderInput={(params) => (
                                        <TextField
                                            {...params}
                                            fullWidth
                                            size="small"
                                            label="Descripción (Catálogo)"
                                            required
                                            error={Boolean(fieldState.error)}
                                            helperText={fieldState.error?.message}
                                        />
                                    )}
                                />
                            )}
                        />
                    </Box>

                    {/* Marca y modelo llegan rellenos al elegir una sugerencia
                        del catálogo, pero siguen siendo editables. */}
                    <Box sx={{ gridColumn: { xs: 'span 1', sm: 'span 3' } }}>
                        <CampoFormulario control={control} nombre="marca" etiqueta="Marca" size="small" mayusculas />
                    </Box>

                    <Box sx={{ gridColumn: { xs: 'span 1', sm: 'span 3' } }}>
                        <CampoFormulario control={control} nombre="modelo" etiqueta="Modelo" size="small" mayusculas />
                    </Box>

                    <Box sx={{ gridColumn: { xs: 'span 1', sm: 'span 3' } }}>
                        <CampoFormulario control={control} nombre="serie" etiqueta="No. de Serie" size="small" mayusculas />
                    </Box>

                    <Box sx={{ gridColumn: { xs: 'span 1', sm: 'span 3' } }}>
                        <CampoFormulario control={control} nombre="noResguardo" etiqueta="No. de Resguardo" size="small" mayusculas />
                    </Box>

                </Box>

                <Divider sx={{ my: 4 }} />

                <Typography variant="h6" sx={{ color: 'primary.main', fontWeight: 'bold', mb: 2 }}>
                    2. Datos del Usuario
                </Typography>
                <Grid container spacing={2}>
                    <Grid size={{ xs: 12, sm: 6 }}>
                        <CampoFormulario control={control} nombre="nombreUsuario" etiqueta="Nombre del Usuario" size="small" mayusculas />
                    </Grid>
                    <Grid size={{ xs: 12, sm: 6 }}>
                        <CampoFormulario control={control} nombre="telefonoUsuario" etiqueta="Teléfono / Ext" size="small" />
                    </Grid>
                    <Grid size={{ xs: 12, sm: 6 }}>
                        <CampoFormulario control={control} nombre="direccionUsuario" etiqueta="Dirección" size="small" mayusculas />
                    </Grid>
                    <Grid size={{ xs: 12, sm: 6 }}>
                        <CampoFormulario control={control} nombre="departamentoUsuario" etiqueta="Departamento" size="small" mayusculas />
                    </Grid>
                    <Grid size={{ xs: 12, sm: 6 }}>
                        <CampoFormulario control={control} nombre="tipoReporte" etiqueta="Tipo de Reporte / Servicio" size="small" mayusculas />
                    </Grid>
                </Grid>

                <Divider sx={{ my: 4 }} />

                <Typography variant="h6" sx={{ color: 'primary.main', fontWeight: 'bold', mb: 2 }}>
                    3. Análisis Técnico
                </Typography>
                <Grid container spacing={2}>
                    <Grid size={{ xs: 12 }}>
                        <CampoFormulario
                            control={control}
                            nombre="fallaReportada"
                            etiqueta="Descripción de la Falla"
                            maximo={1000}
                            mayusculas
                            multiline
                            minRows={2}
                        />
                    </Grid>
                    <Grid size={{ xs: 12 }}>
                        <CampoFormulario
                            control={control}
                            nombre="diagnostico"
                            etiqueta="Diagnóstico Técnico"
                            maximo={1000}
                            obligatorio
                            mayusculas
                            multiline
                            minRows={3}
                        />
                    </Grid>
                </Grid>

                <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 4, pt: 2, borderTop: '1px solid', borderColor: 'divider' }}>
                    <Button
                        type="submit"
                        disabled={generando || isSubmitting}
                        variant="contained"
                        startIcon={<PictureAsPdfIcon />}
                        sx={{ px: 4, py: 1.5, fontWeight: 'bold' }}
                    >
                        Generar Dictamen Oficial
                    </Button>
                </Box>
            </form>
        </Paper>
    );
}
