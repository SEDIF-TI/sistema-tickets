import { useState } from 'react';
import { 
    Box, Typography, Paper, TextField, Button, Grid,
    IconButton, Table, TableBody, TableCell, TableContainer, TableHead, TableRow 
} from '@mui/material';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import DeleteIcon from '@mui/icons-material/Delete';
import AddCircleIcon from '@mui/icons-material/AddCircle';


export default function RequisicionFormato({ solicitarPdf, generando = false }) {
    const [requisicion, setRequisicion] = useState({
        areaSolicitante: 'Soporte Técnico',
        fechaRequerida: '',
        justificacion: '',
        listaArticulos: [{ cantidad: '', unidad: '', descripcion: '' }] 
    });

    const handleChange = (e) => setRequisicion({ ...requisicion, [e.target.name]: e.target.value });

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

    return (
        <Paper sx={{ p: 4, borderRadius: 2 }}>
            <Typography variant="h6" sx={{ mb: 3, color: 'primary.main' }}>Solicitud de Insumos</Typography>
            <Grid container spacing={3}>
                <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField fullWidth label="Área Solicitante" name="areaSolicitante" value={requisicion.areaSolicitante} onChange={handleChange} />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField fullWidth type="date" label="Fecha Requerida" name="fechaRequerida" InputLabelProps={{ shrink: true }} value={requisicion.fechaRequerida} onChange={handleChange} />
                </Grid>
                <Grid size={{ xs: 12 }}>
                    <TextField fullWidth label="Justificación de la compra" name="justificacion" value={requisicion.justificacion} onChange={handleChange} />
                </Grid>
                
                <Grid size={{ xs: 12 }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1, mt: 2 }}>
                        <Typography variant="subtitle1" fontWeight="bold">Artículos Requeridos</Typography>
                        <Button size="small" startIcon={<AddCircleIcon />} onClick={handleAddArticulo} color="primary">Agregar Fila</Button>
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

                <Grid size={{ xs: 12 }} sx={{ display: 'flex', justifyContent: 'flex-end', mt: 3 }}>
                    <Button disabled={generando} 
                        variant="contained" 
                        onClick={() => solicitarPdf('requisicion', requisicion, `Requisicion.pdf`)} 
                        startIcon={<PictureAsPdfIcon />} 
                        sx={{ bgcolor: 'primary.main', px: 4, py: 1.5 }}
                    >
                        Generar PDF
                    </Button>
                </Grid>
            </Grid>
        </Paper>
    );
}