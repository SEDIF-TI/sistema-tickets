import React, { useState } from 'react';
import { Typography, Paper, TextField, Button, Grid, Box } from '@mui/material';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';

const COLOR_GUINDA = '#801A36';

export default function MemorandumFormato({ solicitarPdf }) {
    const [memorandum, setMemorandum] = useState({
        para: '', de: 'Área de Soporte Técnico', asunto: '', cuerpo: ''
    });

    const handleChange = (e) => setMemorandum({ ...memorandum, [e.target.name]: e.target.value });

    return (
        <Paper sx={{ p: 4, borderRadius: 2 }}>
            <Typography variant="h6" sx={{ mb: 3, color: COLOR_GUINDA }}>Redactar Memorándum</Typography>
            <Grid container spacing={3}>
                <Grid item xs={12} sm={6}>
                    <TextField fullWidth required label="Para (Destinatario)" name="para" value={memorandum.para} onChange={handleChange} />
                </Grid>
                <Grid item xs={12} sm={6}>
                    <TextField fullWidth required label="De (Remitente)" name="de" value={memorandum.de} onChange={handleChange} />
                </Grid>
                <Grid item xs={12}>
                    <TextField fullWidth required label="Asunto" name="asunto" value={memorandum.asunto} onChange={handleChange} />
                </Grid>
                <Grid item xs={12}>
                    <TextField fullWidth required multiline rows={6} label="Cuerpo del Mensaje" name="cuerpo" value={memorandum.cuerpo} onChange={handleChange} />
                </Grid>
                <Grid item xs={12} sx={{ display: 'flex', justifyContent: 'flex-end', mt: 2 }}>
                    <Button 
                        variant="contained" 
                        onClick={() => solicitarPdf('memorandum', memorandum, `Memo.pdf`)} 
                        startIcon={<PictureAsPdfIcon />} 
                        sx={{ bgcolor: COLOR_GUINDA, px: 4, py: 1.5 }}
                    >
                        Generar PDF
                    </Button>
                </Grid>
            </Grid>
        </Paper>
    );
}