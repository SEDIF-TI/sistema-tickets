import React, { useState } from 'react';
import { 
    Box, Typography, Paper, Tabs, Tab, TextField, Button, Grid, IconButton, Table, TableBody, TableCell, TableContainer, TableHead, TableRow 
} from '@mui/material';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import DeleteIcon from '@mui/icons-material/Delete';
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline';
import api from '../services/api';

const COLOR_GUINDA = '#801A36';

export default function GeneradorDocumentos() {
    const [tabIndex, setTabIndex] = useState(0);

    const [dictamen, setDictamen] = useState({
        folioTicket: '', equipoEvaluado: '', fallaReportada: '', diagnostico: '', conclusion: '' 
    });

    const [memorandum, setMemorandum] = useState({
        para: '', de: 'Área de Soporte Técnico', asunto: '', cuerpo: ''
    });

    // ---> CAMBIO: listaArticulos ahora es un arreglo de objetos <---
    const [requisicion, setRequisicion] = useState({
        areaSolicitante: 'Soporte Técnico',
        fechaRequerida: '',
        justificacion: '',
        listaArticulos: [{ cantidad: '', unidad: '', descripcion: '' }] 
    });

    const handleChangeDictamen = (e) => setDictamen({ ...dictamen, [e.target.name]: e.target.value });
    const handleChangeMemo = (e) => setMemorandum({ ...memorandum, [e.target.name]: e.target.value });
    const handleChangeRequisicion = (e) => setRequisicion({ ...requisicion, [e.target.name]: e.target.value });

    // --- FUNCIONES PARA LA TABLA DINÁMICA DE REQUISICIÓN ---
    const handleAddArticulo = () => {
        setRequisicion({
            ...requisicion,
            listaArticulos: [...requisicion.listaArticulos, { cantidad: '', unidad: '', descripcion: '' }]
        });
    };

    const handleRemoveArticulo = (index) => {
        const nuevaLista = [...requisicion.listaArticulos];
        nuevaLista.splice(index, 1);
        setRequisicion({ ...requisicion, listaArticulos: nuevaLista });
    };

    const handleChangeArticulo = (index, campo, valor) => {
        const nuevaLista = [...requisicion.listaArticulos];
        nuevaLista[index][campo] = valor;
        setRequisicion({ ...requisicion, listaArticulos: nuevaLista });
    };

    const solicitarPdf = async (endpoint, payload, nombreArchivo) => {
        try {
            const response = await api.post(`/v1/documentos/${endpoint}`, payload, { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', nombreArchivo);
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch (error) {
            console.error("Error al generar el PDF:", error);
            alert("Hubo un error al generar el documento. Verifica que el backend esté listo.");
        }
    };

    return (
        <Box sx={{ p: 3, maxWidth: 1000, mx: 'auto' }}>
            <Typography variant="h5" fontWeight="bold" sx={{ color: COLOR_GUINDA, mb: 3 }}>
                Generador de Documentos Oficiales
            </Typography>

            <Paper sx={{ mb: 3, borderRadius: 2, overflow: 'hidden' }}>
                <Tabs 
                    value={tabIndex} 
                    onChange={(e, newValue) => setTabIndex(newValue)} 
                    centered 
                    TabIndicatorProps={{ style: { backgroundColor: COLOR_GUINDA } }}
                    sx={{ '& .Mui-selected': { color: `${COLOR_GUINDA} !important`, fontWeight: 'bold' } }}
                >
                    <Tab label="Dictamen Técnico" />
                    <Tab label="Memorándum" />
                    <Tab label="Requisición de Material" />
                </Tabs>
            </Paper>

            {/* PESTAÑA 0: DICTAMEN TÉCNICO */}
            {tabIndex === 0 && (
                <Paper sx={{ p: 4, borderRadius: 2 }}>
                    <Typography variant="h6" sx={{ mb: 3, color: COLOR_GUINDA }}>Detalles del Dictamen</Typography>
                    <Grid container spacing={3}>
                        <Grid item xs={12} sm={6}>
                            <TextField fullWidth label="Folio del Ticket Asociado" name="folioTicket" value={dictamen.folioTicket} onChange={handleChangeDictamen} />
                        </Grid>
                        <Grid item xs={12} sm={6}>
                            <TextField fullWidth label="Equipo / Activo Evaluado" name="equipoEvaluado" value={dictamen.equipoEvaluado} onChange={handleChangeDictamen} />
                        </Grid>
                        <Grid item xs={12}>
                            <TextField fullWidth label="Falla Reportada" name="fallaReportada" value={dictamen.fallaReportada} onChange={handleChangeDictamen} />
                        </Grid>
                        <Grid item xs={12}>
                            <TextField fullWidth multiline rows={4} label="Diagnóstico Técnico Detallado" name="diagnostico" value={dictamen.diagnostico} onChange={handleChangeDictamen} />
                        </Grid>
                        <Grid item xs={12}>
                            <TextField fullWidth multiline rows={2} label="Conclusión / Estado Final (Max 200 caracteres)" name="conclusion" value={dictamen.conclusion} onChange={handleChangeDictamen} inputProps={{ maxLength: 200 }} helperText={`${dictamen.conclusion.length}/200`} />
                        </Grid>
                        <Grid item xs={12} sx={{ display: 'flex', justifyContent: 'flex-end', mt: 2 }}>
                            <Button variant="contained" onClick={() => solicitarPdf('dictamen', dictamen, `Dictamen_${dictamen.folioTicket}.pdf`)} startIcon={<PictureAsPdfIcon />} sx={{ bgcolor: COLOR_GUINDA, px: 4, py: 1.5 }}>Generar PDF</Button>
                        </Grid>
                    </Grid>
                </Paper>
            )}

            {/* PESTAÑA 1: MEMORÁNDUM */}
            {tabIndex === 1 && (
                <Paper sx={{ p: 4, borderRadius: 2 }}>
                    <Typography variant="h6" sx={{ mb: 3, color: COLOR_GUINDA }}>Redactar Memorándum</Typography>
                    <Grid container spacing={3}>
                        <Grid item xs={12} sm={6}>
                            <TextField fullWidth required label="Para (Destinatario)" name="para" value={memorandum.para} onChange={handleChangeMemo} />
                        </Grid>
                        <Grid item xs={12} sm={6}>
                            <TextField fullWidth required label="De (Remitente)" name="de" value={memorandum.de} onChange={handleChangeMemo} />
                        </Grid>
                        <Grid item xs={12}>
                            <TextField fullWidth required label="Asunto" name="asunto" value={memorandum.asunto} onChange={handleChangeMemo} />
                        </Grid>
                        <Grid item xs={12}>
                            <TextField fullWidth required multiline rows={6} label="Cuerpo del Mensaje" name="cuerpo" value={memorandum.cuerpo} onChange={handleChangeMemo} />
                        </Grid>
                        <Grid item xs={12} sx={{ display: 'flex', justifyContent: 'flex-end', mt: 2 }}>
                            <Button variant="contained" onClick={() => solicitarPdf('memorandum', memorandum, `Memo.pdf`)} startIcon={<PictureAsPdfIcon />} sx={{ bgcolor: COLOR_GUINDA, px: 4, py: 1.5 }}>Generar PDF</Button>
                        </Grid>
                    </Grid>
                </Paper>
            )}

            {/* PESTAÑA 2: REQUISICIÓN DE MATERIAL (CON TABLA DINÁMICA) */}
            {tabIndex === 2 && (
                <Paper sx={{ p: 4, borderRadius: 2 }}>
                    <Typography variant="h6" sx={{ mb: 3, color: COLOR_GUINDA }}>Solicitud de Insumos</Typography>
                    <Grid container spacing={3}>
                        <Grid item xs={12} sm={6}>
                            <TextField fullWidth label="Área Solicitante" name="areaSolicitante" value={requisicion.areaSolicitante} onChange={handleChangeRequisicion} />
                        </Grid>
                        <Grid item xs={12} sm={6}>
                            <TextField fullWidth type="date" label="Fecha Requerida" name="fechaRequerida" InputLabelProps={{ shrink: true }} value={requisicion.fechaRequerida} onChange={handleChangeRequisicion} />
                        </Grid>
                        <Grid item xs={12}>
                            <TextField fullWidth label="Justificación de la compra" name="justificacion" value={requisicion.justificacion} onChange={handleChangeRequisicion} />
                        </Grid>
                        
                        {/* SECCIÓN DE LA TABLA DINÁMICA */}
                        <Grid item xs={12}>
                            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1, mt: 2 }}>
                                <Typography variant="subtitle1" fontWeight="bold">Artículos Requeridos</Typography>
                                <Button size="small" startIcon={<AddCircleOutlineIcon />} onClick={handleAddArticulo} color="primary">Agregar Fila</Button>
                            </Box>
                            
                            <TableContainer component={Paper} variant="outlined">
                                <Table size="small">
                                    <TableHead sx={{ bgcolor: '#f5f5f5' }}>
                                        <TableRow>
                                            <TableCell width="15%">Cantidad</TableCell>
                                            <TableCell width="20%">Unidad</TableCell>
                                            <TableCell width="55%">Descripción</TableCell>
                                            <TableCell width="10%" align="center">Acción</TableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {requisicion.listaArticulos.map((articulo, index) => (
                                            <TableRow key={index}>
                                                <TableCell>
                                                    <TextField size="small" type="number" fullWidth value={articulo.cantidad} onChange={(e) => handleChangeArticulo(index, 'cantidad', e.target.value)} />
                                                </TableCell>
                                                <TableCell>
                                                    <TextField size="small" placeholder="Ej: Pieza, Caja" fullWidth value={articulo.unidad} onChange={(e) => handleChangeArticulo(index, 'unidad', e.target.value)} />
                                                </TableCell>
                                                <TableCell>
                                                    <TextField size="small" placeholder="Descripción detallada del artículo" fullWidth value={articulo.descripcion} onChange={(e) => handleChangeArticulo(index, 'descripcion', e.target.value)} />
                                                </TableCell>
                                                <TableCell align="center">
                                                    <IconButton color="error" onClick={() => handleRemoveArticulo(index)} disabled={requisicion.listaArticulos.length === 1}>
                                                        <DeleteIcon />
                                                    </IconButton>
                                                </TableCell>
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            </TableContainer>
                        </Grid>

                        <Grid item xs={12} sx={{ display: 'flex', justifyContent: 'flex-end', mt: 3 }}>
                            <Button variant="contained" onClick={() => solicitarPdf('requisicion', requisicion, `Requisicion.pdf`)} startIcon={<PictureAsPdfIcon />} sx={{ bgcolor: COLOR_GUINDA, px: 4, py: 1.5 }}>
                                Generar PDF
                            </Button>
                        </Grid>
                    </Grid>
                </Paper>
            )}
        </Box>
    );
}