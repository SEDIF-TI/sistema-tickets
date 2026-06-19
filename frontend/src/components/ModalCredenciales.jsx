import React from 'react';
import { 
    Dialog, DialogTitle, DialogContent, DialogActions, 
    Button, Typography, Box, Paper, Divider 
} from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import PrintIcon from '@mui/icons-material/Print';

const COLOR_GUINDA = '#801A36';

export default function ModalCredenciales({ open, onClose, usuarioData }) {
    
    const handleImprimir = () => {
        const printWindow = window.open('', '', 'width=600,height=400');
        
        const htmlTemplate = `
            <html>
                <head>
                    <title>Credenciales SEDIF</title>
                    <style>
                        body { font-family: 'Arial', sans-serif; padding: 20px; color: #333; }
                        .tarjeta { 
                            border: 2px dashed #ccc; 
                            padding: 30px; 
                            max-width: 400px; 
                            margin: 0 auto; 
                            text-align: center;
                            border-radius: 10px;
                        }
                        .header { color: ${COLOR_GUINDA}; font-size: 22px; font-weight: bold; margin-bottom: 10px; }
                        .subtitle { font-size: 14px; color: #666; margin-bottom: 20px; }
                        .dato-box { margin: 15px 0; padding: 10px; background-color: #f9f9f9; border-radius: 5px; }
                        .label { font-size: 12px; text-transform: uppercase; color: #888; }
                        .valor { font-size: 18px; font-weight: bold; margin-top: 5px; }
                        .password { font-size: 24px; color: ${COLOR_GUINDA}; letter-spacing: 2px; }
                        .footer { margin-top: 25px; font-size: 11px; color: #999; border-top: 1px solid #eee; padding-top: 10px; }
                        
                        @page { margin: 0; }
                        @media print { body { padding: 50px; } }
                    </style>
                </head>
                <body>
                    <div class="tarjeta">
                        <div class="header">SEDIF</div>
                        <div class="subtitle">Sistema de Tickets y Soporte</div>
                        
                        <div style="text-align: left; margin-top: 20px;">
                            <p style="margin: 5px 0;"><strong>Nombre:</strong> ${usuarioData?.nombre}</p>
                            <p style="margin: 5px 0;"><strong>Área:</strong> ${usuarioData?.area}</p>
                            <p style="margin: 5px 0;"><strong>Rol:</strong> ${usuarioData?.rol}</p>
                        </div>

                        <div class="dato-box">
                            <div class="label">Usuario / Correo Electrónico</div>
                            <div class="valor">${usuarioData?.correo}</div>
                        </div>

                        <div class="dato-box" style="border: 2px solid ${COLOR_GUINDA};">
                            <div class="label" style="color: ${COLOR_GUINDA};">Contraseña Temporal</div>
                            <div class="valor password">${usuarioData?.password}</div>
                        </div>

                        <div class="footer">
                            <strong>CONFIDENCIAL:</strong> Entregue este documento al usuario. <br/>
                            El sistema solicitará el cambio de esta contraseña al iniciar sesión.
                        </div>
                    </div>
                    <script>
                        window.onload = function() { 
                            window.print();
                            setTimeout(function() { window.close(); }, 500);
                        }
                    </script>
                </body>
            </html>
        `;
        
        printWindow.document.write(htmlTemplate);
        printWindow.document.close();
    };

    return (
        <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth PaperProps={{ sx: { borderRadius: 3, p: 1 } }}>
            <DialogTitle sx={{ textAlign: 'center', pb: 1 }}>
                <CheckCircleIcon sx={{ fontSize: 60, color: '#2ecc71', mb: 1 }} />
                <Typography variant="h5" fontWeight="bold">¡Usuario Registrado!</Typography>
            </DialogTitle>
            
            <DialogContent>
                <Typography textAlign="center" color="textSecondary" mb={3}>
                    El empleado ha sido dado de alta correctamente en el sistema.
                </Typography>

                <Paper elevation={0} sx={{ bgcolor: '#f5f6fa', p: 3, borderRadius: 2, border: '1px solid #e0e0e0' }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
                        <Typography variant="body2" color="textSecondary">Usuario / Correo:</Typography>
                        <Typography variant="body1" fontWeight="bold">{usuarioData?.correo}</Typography>
                    </Box>
                    <Divider sx={{ mb: 2 }} />
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <Typography variant="body2" color="textSecondary">Contraseña Temporal:</Typography>
                        <Typography variant="h5" fontWeight="bold" sx={{ color: COLOR_GUINDA, letterSpacing: 2 }}>
                            {usuarioData?.password}
                        </Typography>
                    </Box>
                </Paper>
                
                <Typography variant="body2" textAlign="center" color="error" sx={{ mt: 3, fontWeight: 'medium' }}>
                    Por favor, imprima estas credenciales y entréguelas en un sobre cerrado al empleado.
                </Typography>
            </DialogContent>

            <DialogActions sx={{ justifyContent: 'center', pb: 3, gap: 2 }}>
                <Button onClick={onClose} variant="outlined" color="inherit" sx={{ px: 4 }}>Cerrar</Button>
                <Button onClick={handleImprimir} variant="contained" startIcon={<PrintIcon />} sx={{ bgcolor: COLOR_GUINDA, '&:hover': { bgcolor: '#5e1026' }, px: 4 }}>
                    Imprimir Credenciales
                </Button>
            </DialogActions>
        </Dialog>
    );
}