import { useState, useEffect, useContext } from 'react';
import { Typography, Paper, TextField, Button, Grid, Box, Divider, Autocomplete } from '@mui/material';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import { toUpper } from '../../util/formater';
import api from '../../services/api';
import { AuthContext } from '../../context/AuthContext.jsx';


/**
 * Dictamen técnico de un equipo.
 *
 * Todo el documento se arma en un solo objeto de estado, que es el que viaja
 * como payload a `solicitarPdf('dictamen', …)`: el backend compone el PDF con
 * esos campos y registra el dictamen emitido.
 *
 * La descripción del equipo se captura con autocompletado contra el catálogo:
 * elegir una sugerencia rellena marca y modelo, y al generar el documento la
 * descripción escrita se da de alta o se actualiza en el catálogo, de modo que
 * esté disponible en el siguiente dictamen.
 */
export default function DictamenFormato({ solicitarPdf, generando = false }) {
    // El técnico que firma sale de la sesión, no se teclea.
    const { user } = useContext(AuthContext);
    const nombreTecnico = user?.nombre || 'Técnico de Soporte';

    const [dictamen, setDictamen] = useState({
        // Los rellena el sistema; en pantalla se muestran deshabilitados.
        folioTicket: '',
        fecha: new Date().toLocaleDateString('es-MX'),

        cve: 'OT', descripcionEquipo: '', marca: '', modelo: '', serie: '', noResguardo: '',

        direccionUsuario: '', departamentoUsuario: '', nombreUsuario: '', telefonoUsuario: '', tipoReporte: '',

        fallaReportada: '', diagnostico: '',

        // No tienen campo en la vista, pero el DTO del backend los espera.
        concepto: '', observacion: '',

        // Firmas del formato oficial: el técnico sale de la sesión y los dos
        // cargos son fijos por estructura del área.
        realizadoPor: nombreTecnico,
        revisadoPor: 'C. MARCO POLO OLIVARES GONZALEZ',
        recibidoPor: 'DRA. CARMEN GONZÁLEZ SERDÁN'
    });

    // Sugerencias del catálogo y texto que las busca.
    const [opcionesEquipo, setOpcionesEquipo] = useState([]);
    const [busquedaEquipo, setBusquedaEquipo] = useState('');

    // La consulta al catálogo espera 300 ms desde la última tecla, y no arranca
    // hasta el segundo carácter: con uno solo la lista devuelta no acota nada.
    useEffect(() => {
        if (busquedaEquipo.length < 2) {
            setOpcionesEquipo([]);
            return;
        }
        const fetchEquipos = async () => {
            try {
                const res = await api.get(`/v1/equipos/buscar?q=${busquedaEquipo}`);
                setOpcionesEquipo(res.data);
            } catch (error) {
                console.error("Error buscando equipos:", error);
            }
        };
        const timeoutId = setTimeout(() => fetchEquipos(), 300);
        return () => clearTimeout(timeoutId);
    }, [busquedaEquipo]);

    const handleChange = (e) => {
        setDictamen({ ...dictamen, [e.target.name]: toUpper(e.target.value) });
    };

    return (
        <Paper sx={{ p: 4, borderRadius: 2, boxShadow: 2 }}>
            <Typography variant="h6" sx={{ color: 'primary.main', fontWeight: 'bold', mb: 2 }}>
                1. Datos Generales y del Equipo
            </Typography>

            {/* Rejilla de 12 columnas: los tramos de cada campo reparten esta
                sección en dos filas de anchos desiguales. */}
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: 'repeat(1, 1fr)', sm: 'repeat(12, 1fr)' }, gap: 2 }}>

                <Box sx={{ gridColumn: { xs: 'span 1', sm: 'span 2' } }}>
                    <TextField fullWidth size="small" label="Folio Ticket" value="Autogenerado" disabled sx={{ bgcolor: '#f8fafc' }} />
                </Box>
                
                <Box sx={{ gridColumn: { xs: 'span 1', sm: 'span 2' } }}>
                    <TextField fullWidth size="small" label="Fecha" value={dictamen.fecha} disabled sx={{ bgcolor: '#f8fafc' }}/>
                </Box>
                
                <Box sx={{ gridColumn: { xs: 'span 1', sm: 'span 2' } }}>
                    <TextField fullWidth size="small" label="CVE (Ej: OT)" name="cve" value={dictamen.cve} onChange={handleChange} />
                </Box>

                {/* La descripción ocupa media rejilla: es el campo más largo y
                    además despliega las sugerencias del catálogo. */}
                <Box sx={{ gridColumn: { xs: 'span 1', sm: 'span 6' } }}>
                    <Autocomplete
                        freeSolo
                        options={opcionesEquipo}
                        getOptionLabel={(option) => typeof option === 'string' ? option : option.descripcion}
                        inputValue={dictamen.descripcionEquipo}
                        onInputChange={(event, newInputValue) => {
                            setBusquedaEquipo(newInputValue);
                            setDictamen({ ...dictamen, descripcionEquipo: toUpper(newInputValue) });
                        }}
                        onChange={(event, newValue) => {
                            if (typeof newValue === 'object' && newValue !== null) {
                                setDictamen({
                                    ...dictamen,
                                    descripcionEquipo: newValue.descripcion || '',
                                    marca: newValue.marca || '',
                                    modelo: newValue.modelo || ''
                                });
                            } else if (typeof newValue === 'string') {
                                setDictamen({ ...dictamen, descripcionEquipo: toUpper(newValue) });
                            }
                        }}
                        renderInput={(params) => (
                            <TextField {...params} fullWidth size="small" label="Descripción (Catálogo)" />
                        )}
                    />
                </Box>

                {/* Marca y modelo llegan rellenos al elegir una sugerencia del
                    catálogo, pero siguen siendo editables. */}
                <Box sx={{ gridColumn: { xs: 'span 1', sm: 'span 3' } }}>
                    <TextField fullWidth size="small" label="Marca" name="marca" value={dictamen.marca} onChange={handleChange} />
                </Box>
                
                <Box sx={{ gridColumn: { xs: 'span 1', sm: 'span 3' } }}>
                    <TextField fullWidth size="small" label="Modelo" name="modelo" value={dictamen.modelo} onChange={handleChange} />
                </Box>
                
                <Box sx={{ gridColumn: { xs: 'span 1', sm: 'span 3' } }}>
                    <TextField fullWidth size="small" label="No. de Serie" name="serie" value={dictamen.serie} onChange={handleChange} />
                </Box>
                
                <Box sx={{ gridColumn: { xs: 'span 1', sm: 'span 3' } }}>
                    <TextField fullWidth size="small" label="No. de Resguardo" name="noResguardo" value={dictamen.noResguardo} onChange={handleChange} />
                </Box>

            </Box>

            <Divider sx={{ my: 4 }} />

            <Typography variant="h6" sx={{ color: 'primary.main', fontWeight: 'bold', mb: 2 }}>
                2. Datos del Usuario
            </Typography>
            <Grid container spacing={2}>
                <Grid size={{ xs: 12, sm: 6 }}><TextField fullWidth size="small" label="Nombre del Usuario" name="nombreUsuario" value={dictamen.nombreUsuario} onChange={handleChange} /></Grid>
                <Grid size={{ xs: 12, sm: 6 }}><TextField fullWidth size="small" label="Teléfono / Ext" name="telefonoUsuario" value={dictamen.telefonoUsuario} onChange={handleChange} /></Grid>
                <Grid size={{ xs: 12, sm: 6 }}><TextField fullWidth size="small" label="Dirección" name="direccionUsuario" value={dictamen.direccionUsuario} onChange={handleChange} /></Grid>
                <Grid size={{ xs: 12, sm: 6 }}><TextField fullWidth size="small" label="Departamento" name="departamentoUsuario" value={dictamen.departamentoUsuario} onChange={handleChange} /></Grid>
                <Grid size={{ xs: 12, sm: 6 }}><TextField fullWidth size="small" label="Tipo de Reporte / Servicio" name="tipoReporte" value={dictamen.tipoReporte} onChange={handleChange} /></Grid>
            </Grid>

            <Divider sx={{ my: 4 }} />

            <Typography variant="h6" sx={{ color: 'primary.main', fontWeight: 'bold', mb: 2 }}>
                3. Análisis Técnico
            </Typography>
            <Grid container spacing={2}>
                <Grid size={{ xs: 12 }}><TextField fullWidth multiline minRows={2} label="Descripción de la Falla" name="fallaReportada" value={dictamen.fallaReportada} onChange={handleChange} /></Grid>
                <Grid size={{ xs: 12 }}><TextField fullWidth multiline minRows={3} label="Diagnóstico Técnico" name="diagnostico" value={dictamen.diagnostico} onChange={handleChange} /></Grid>
            </Grid>

            <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 4, pt: 2, borderTop: '1px solid #eee' }}>
                <Button disabled={generando} 
                    variant="contained" 
                    onClick={async () => {
                        // El equipo se registra en el catálogo antes de emitir
                        // el documento, para tenerlo como sugerencia la próxima
                        // vez. Si falla, el dictamen se genera igual: el
                        // catálogo es una comodidad, no un requisito.
                        try {
                            if (dictamen.descripcionEquipo) {
                                await api.post('/v1/equipos/upsert', {
                                    descripcion: dictamen.descripcionEquipo,
                                    marca: dictamen.marca,
                                    modelo: dictamen.modelo
                                });
                            }
                        } catch (e) { console.error("No se pudo actualizar catálogo JIT", e); }

                        // La firma del técnico se resuelve aquí y no en el estado
                        // inicial: ese se fija en el primer render, cuando la
                        // sesión puede no haberse recuperado todavía.
                        const firmaTecnico = dictamen.realizadoPor || nombreTecnico;

                        // El tratamiento "C. " se antepone a los nombres de las
                        // firmas solo en la copia que va al PDF, porque es una
                        // exigencia del formato oficial y no del dato: el
                        // estado del formulario conserva el nombre limpio.
                        const datosParaPdf = {
                            ...dictamen,
                            realizadoPor: firmaTecnico.startsWith('C. ') ? firmaTecnico : `C. ${firmaTecnico}`,
                            nombreUsuario: dictamen.nombreUsuario && !dictamen.nombreUsuario.startsWith('C. ')
                                            ? `C. ${dictamen.nombreUsuario}`
                                            : dictamen.nombreUsuario
                        };

                        solicitarPdf('dictamen', datosParaPdf, `Dictamen_Automatico.pdf`);
                    }}
                    startIcon={<PictureAsPdfIcon />}
                    sx={{ bgcolor: 'primary.main', '&:hover': { bgcolor: '#5e1227' }, px: 4, py: 1.5, fontWeight: 'bold' }}
                >
                    Generar Dictamen Oficial
                </Button>
            </Box>
        </Paper>
    );
}