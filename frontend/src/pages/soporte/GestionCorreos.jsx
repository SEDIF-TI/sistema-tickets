import React, { useState, useEffect } from 'react';
import { 
  Box, Paper, Typography, Button, TextField, MenuItem, Table, TableBody, TableCell, 
  TableContainer, TableHead, TableRow, Chip, IconButton, Tooltip, Dialog, DialogTitle, 
  DialogContent, DialogActions, Grid, Alert, Snackbar, CircularProgress, InputAdornment, 
  Card, CardContent, Divider, Stack, useTheme 
} from '@mui/material';
import {
  Add as AddIcon, Search as SearchIcon, Edit as EditIcon, Delete as DeleteIcon,
  PauseCircleOutlined as PauseIcon, CheckCircleOutlined as CheckIcon, Email as EmailIcon,
  Refresh as RefreshIcon, MarkEmailRead as EmailReadIcon, Block as BlockIcon,
  PictureAsPdf as PdfIcon // Agregamos el ícono de PDF
} from '@mui/icons-material';

import { correoService } from '../../services/correoService';
import api from '../../services/api'; // Importamos tu instancia de axios
import { toUpper } from '../../util/formater'; // Importamos tu formateador

const INITIAL_FORM_STATE = {
  id: null,
  nombre: '',
  apellidoPaterno: '',
  apellidoMaterno: '',
  area: '',
  cargo: '',
  extension: '',
  correo: '',
  estado: 'ACTIVO',
  cuotaAlmacenamiento: '5 GB',
};

export const GestionCorreos = () => {
  const theme = useTheme();

  // Estados de datos
  const [correos, setCorreos] = useState([]);
  const [loading, setLoading] = useState(false);

  // Filtros
  const [filtroTexto, setFiltroTexto] = useState('');
  const [filtroEstado, setFiltroEstado] = useState('');

  // Modal y Formulario
  const [modalAbierto, setModalAbierto] = useState(false);
  const [modoEdicion, setModoEdicion] = useState(false);
  const [formData, setFormData] = useState(INITIAL_FORM_STATE);

  // Alertas / Mensajes (Snackbar)
  const [snackbar, setSnackbar] = useState({ open: false, mensaje: '', tipo: 'info' });

  useEffect(() => {
    cargarCorreos();
  }, [filtroEstado]);

  const cargarCorreos = async (texto = filtroTexto, estado = filtroEstado) => {
    setLoading(true);
    try {
      const data = await correoService.obtenerCorreos(texto, estado);
      setCorreos(data);
    } catch (err) {
      mostrarMensaje(err.message || 'Error al obtener los correos', 'error');
    } finally {
      setLoading(false);
    }
  };

  const mostrarMensaje = (mensaje, tipo = 'info') => {
    setSnackbar({ open: true, mensaje, tipo });
  };

  const handleCerrarSnackbar = () => {
    setSnackbar({ ...snackbar, open: false });
  };

  const handleBuscar = (e) => {
    e.preventDefault();
    cargarCorreos(filtroTexto, filtroEstado);
  };

  const handleAbrirModalCrear = () => {
    setFormData(INITIAL_FORM_STATE);
    setModoEdicion(false);
    setModalAbierto(true);
  };

  const handleAbrirModalEditar = (item) => {
    setFormData({
      id: item.id,
      nombre: item.nombre || '',
      apellidoPaterno: item.apellidoPaterno || '',
      apellidoMaterno: item.apellidoMaterno || '',
      area: item.area || '',
      cargo: item.cargo || '',
      extension: item.extension || '',
      correo: item.correo || '',
      estado: item.estado || 'ACTIVO',
      cuotaAlmacenamiento: item.cuotaAlmacenamiento || '5 GB',
    });
    setModoEdicion(true);
    setModalAbierto(true);
  };

  // 1. ESTANDARIZACIÓN A MAYÚSCULAS CON EL FORMATEADOR
  const handleInputChange = (e) => {
    const { name, value } = e.target;
    // Si el campo es 'correo', lo forzamos a minúsculas por limpieza. 
    // Todo lo demás pasa por el formateador toUpper.
    const formattedValue = name === 'correo' ? value.toLowerCase() : toUpper(value);
    
    setFormData((prev) => ({ ...prev, [name]: formattedValue }));
  };

  const handleGuardar = async (e) => {
    e.preventDefault();
    try {
      if (modoEdicion) {
        await correoService.actualizarCorreo(formData.id, formData);
        mostrarMensaje('Correo actualizado correctamente', 'success');
      } else {
        await correoService.crearCorreo(formData);
        mostrarMensaje('Correo registrado exitosamente', 'success');
      }
      setModalAbierto(false);
      cargarCorreos();
    } catch (err) {
      mostrarMensaje(err.message || 'Error al guardar el registro', 'error');
    }
  };

  const handleCambiarEstado = async (id, nuevoEstado) => {
    try {
      await correoService.cambiarEstado(id, nuevoEstado);
      mostrarMensaje(`Estado cambiado a ${nuevoEstado}`, 'info');
      cargarCorreos();
    } catch (err) {
      mostrarMensaje(err.message || 'Error al cambiar estado', 'error');
    }
  };

  const handleEliminar = async (id) => {
    if (!window.confirm('¿Está seguro de eliminar este registro de correo?')) return;
    try {
      await correoService.eliminarCorreo(id);
      mostrarMensaje('Registro eliminado correctamente', 'warning');
      cargarCorreos();
    } catch (err) {
      mostrarMensaje(err.message || 'Error al eliminar', 'error');
    }
  };

  // 2. LÓGICA PARA GENERAR Y DESCARGAR EL DICTAMEN EN PDF
  const handleGenerarDictamen = async (row) => {
    try {
      mostrarMensaje('Generando documento PDF...', 'info');
      
      const usuarioLogueado = JSON.parse(localStorage.getItem('user')) || {};
      const nombreTecnico = usuarioLogueado.nombre || 'SOPORTE TÉCNICO';
      const nombreCompletoUsuario = `${row.nombre} ${row.apellidoPaterno} ${row.apellidoMaterno}`.trim();

      // Mapeamos los datos del correo al DictamenRequest
      const dictamenPayload = {
        folioTicket: row.id || 0, // Usamos el ID del registro como folio
        fecha: new Date().toLocaleDateString('es-MX'),
        
        // Datos del Usuario
        direccionUsuario: 'N/A',
        departamentoUsuario: row.area || 'N/A',
        nombreUsuario: `C. ${nombreCompletoUsuario}`,
        telefonoUsuario: row.extension || 'N/A',
        tipoReporte: 'GESTIÓN Y ASIGNACIÓN DE CORREO INSTITUCIONAL',
        
        // Datos del "Equipo" (Adaptado a Correo)
        cve: 'N/A',
        descripcionEquipo: `CUENTA DE CORREO: ${row.correo}`,
        marca: `CUOTA: ${row.cuotaAlmacenamiento}`,
        modelo: 'N/A',
        serie: 'N/A',
        noResguardo: 'N/A',
        
        // Análisis Técnico
        fallaReportada: `SOLICITUD PARA CUENTA DE CORREO INSTITUCIONAL. ESTADO: ${row.estado}`,
        diagnostico: `VERIFICACIÓN Y CONFIGURACIÓN DE PARÁMETROS EN EL SERVIDOR DE CORREOS.`,
        hallazgos: `CARGO: ${row.cargo || 'N/A'}`,
        conclusion: `SE PROCESÓ LA SOLICITUD DE LA CUENTA DE CORREO INSTITUCIONAL DE MANERA EXITOSA.`,

        // Firmas
        realizadoPor: nombreTecnico.startsWith('C. ') ? nombreTecnico : `C. ${nombreTecnico}`,
        revisadoPor: 'C. MARCO POLO OLIVARES GONZALEZ',
      };

      // NOTA: Ajusta la ruta '/api/documentos/dictamen' según la URL exacta de tu controlador
      const response = await api.post('/api/documentos/dictamen', dictamenPayload, { 
        responseType: 'blob' 
      });

      // Crear URL del Blob y forzar descarga
      const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `Dictamen_Correo_${row.correo}.pdf`);
      document.body.appendChild(link);
      link.click();
      
      // Limpieza
      link.parentNode.removeChild(link);
      window.URL.revokeObjectURL(url);
      
      mostrarMensaje('PDF descargado exitosamente', 'success');
    } catch (error) {
      console.error(error);
      mostrarMensaje('Error al descargar el PDF', 'error');
    }
  };

  const getChipColor = (estado) => {
    switch (estado) {
      case 'ACTIVO': return { color: 'success', variant: 'filled' };
      case 'SUSPENDIDO': return { color: 'warning', variant: 'filled' };
      case 'BAJA': return { color: 'error', variant: 'filled' };
      case 'EN_PROCESO': return { color: 'info', variant: 'filled' };
      default: return { color: 'default', variant: 'outlined' };
    }
  };

  const totalActivos = correos.filter((c) => c.estado === 'ACTIVO').length;
  const totalSuspendidos = correos.filter((c) => c.estado === 'SUSPENDIDO').length;

  return (
    <Box sx={{ p: { xs: 2, md: 3 } }}>
      {/* ENCABEZADO Y ACCIONES PRINCIPALES */}
      <Box sx={{ display: 'flex', flexDirection: { xs: 'column', sm: 'row' }, justifyContent: 'space-between', alignItems: { xs: 'flex-start', sm: 'center' }, mb: 3, gap: 2 }}>
        <Box>
          <Typography variant="h5" sx={{ fontWeight: 700, color: 'text.primary' }}>Gestión de Correos Institucionales</Typography>
          <Typography variant="body2" color="text.secondary">Administración y control de cuentas corporativas asignadas al personal</Typography>
        </Box>
        <Button variant="contained" color="primary" startIcon={<AddIcon />} onClick={handleAbrirModalCrear} sx={{ fontWeight: 600, px: 2.5 }}>
          Nuevo Correo
        </Button>
      </Box>

      {/* TARJETAS INFORMATIVAS / METRICAS */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={4}>
          <Card variant="outlined" sx={{ borderColor: 'divider' }}>
            <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2, py: 2 }}>
              <Box sx={{ p: 1.5, borderRadius: 2, bgcolor: 'primary.light', color: 'primary.main', display: 'flex' }}><EmailIcon /></Box>
              <Box>
                <Typography variant="body2" color="text.secondary">Total Registrados</Typography>
                <Typography variant="h6" sx={{ fontWeight: 700 }}>{correos.length}</Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Card variant="outlined" sx={{ borderColor: 'divider' }}>
            <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2, py: 2 }}>
              <Box sx={{ p: 1.5, borderRadius: 2, bgcolor: 'success.light', color: 'success.dark', display: 'flex' }}><EmailReadIcon /></Box>
              <Box>
                <Typography variant="body2" color="text.secondary">Cuentas Activas</Typography>
                <Typography variant="h6" sx={{ fontWeight: 700, color: 'success.main' }}>{totalActivos}</Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Card variant="outlined" sx={{ borderColor: 'divider' }}>
            <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2, py: 2 }}>
              <Box sx={{ p: 1.5, borderRadius: 2, bgcolor: 'warning.light', color: 'warning.dark', display: 'flex' }}><BlockIcon /></Box>
              <Box>
                <Typography variant="body2" color="text.secondary">Suspendidos / Inactivos</Typography>
                <Typography variant="h6" sx={{ fontWeight: 700, color: 'warning.dark' }}>{totalSuspendidos}</Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* BARRA DE BÚSQUEDA Y FILTROS */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <Box component="form" onSubmit={handleBuscar} sx={{ display: 'flex', flexDirection: { xs: 'column', md: 'row' }, gap: 2, alignItems: 'center' }}>
          <TextField
            fullWidth size="small" placeholder="Buscar por nombre, correo o área..."
            value={filtroTexto} onChange={(e) => setFiltroTexto(e.target.value)}
            InputProps={{ startAdornment: (<InputAdornment position="start"><SearchIcon color="action" /></InputAdornment>) }}
          />
          <TextField select size="small" label="Estado" value={filtroEstado} onChange={(e) => setFiltroEstado(e.target.value)} sx={{ minWidth: { xs: '100%', md: 200 } }}>
            <MenuItem value="">Todos los estados</MenuItem>
            <MenuItem value="ACTIVO">ACTIVO</MenuItem>
            <MenuItem value="SUSPENDIDO">SUSPENDIDO</MenuItem>
            <MenuItem value="BAJA">BAJA</MenuItem>
            <MenuItem value="EN_PROCESO">EN PROCESO</MenuItem>
          </TextField>
          <Stack direction="row" spacing={1} sx={{ width: { xs: '100%', md: 'auto' } }}>
            <Button variant="outlined" type="submit" startIcon={<SearchIcon />} sx={{ px: 3 }}>Buscar</Button>
            <IconButton onClick={() => cargarCorreos()} title="Recargar lista"><RefreshIcon /></IconButton>
          </Stack>
        </Box>
      </Paper>

      {/* TABLA DE CORREOS */}
      <TableContainer component={Paper}>
        <Table>
          <TableHead sx={{ bgcolor: 'grey.100' }}>
            <TableRow>
              <TableCell sx={{ fontWeight: 700 }}>Titular</TableCell>
              <TableCell sx={{ fontWeight: 700 }}>Área / Cargo</TableCell>
              <TableCell sx={{ fontWeight: 700 }}>Correo Institucional</TableCell>
              <TableCell sx={{ fontWeight: 700 }}>Ext.</TableCell>
              <TableCell sx={{ fontWeight: 700 }}>Cuota</TableCell>
              <TableCell sx={{ fontWeight: 700 }}>Estado</TableCell>
              <TableCell align="right" sx={{ fontWeight: 700, pr: 3 }}>Acciones</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={7} align="center" sx={{ py: 5 }}>
                  <CircularProgress size={32} sx={{ mr: 1, verticalAlign: 'middle' }} />
                  <Typography variant="body2" color="text.secondary" display="inline">Cargando correos...</Typography>
                </TableCell>
              </TableRow>
            ) : correos.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} align="center" sx={{ py: 5 }}>
                  <Typography variant="body2" color="text.secondary">No se encontraron registros de correos institucionales.</Typography>
                </TableCell>
              </TableRow>
            ) : (
              correos.map((row) => {
                const chipProps = getChipColor(row.estado);
                return (
                  <TableRow key={row.id} hover>
                    <TableCell>
                      <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
                        {row.nombre} {row.apellidoPaterno} {row.apellidoMaterno}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2">{row.area}</Typography>
                      <Typography variant="caption" color="text.secondary">{row.cargo || 'Sin cargo asignado'}</Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" sx={{ fontFamily: 'monospace', fontWeight: 600, color: 'primary.main' }}>
                        {row.correo}
                      </Typography>
                    </TableCell>
                    <TableCell><Typography variant="body2">{row.extension || '-'}</Typography></TableCell>
                    <TableCell><Chip label={row.cuotaAlmacenamiento || 'N/A'} size="small" variant="outlined" /></TableCell>
                    <TableCell>
                      <Chip label={row.estado} size="small" color={chipProps.color} sx={{ fontWeight: 600, fontSize: '0.75rem' }} />
                    </TableCell>
                    <TableCell align="right" sx={{ pr: 2 }}>
                      <Stack direction="row" spacing={0.5} justifyContent="flex-end">
                        
                        {/* 3. BOTÓN PARA DESCARGAR PDF */}
                        <Tooltip title="Descargar Dictamen">
                          <IconButton size="small" color="secondary" onClick={() => handleGenerarDictamen(row)}>
                            <PdfIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>

                        <Tooltip title="Editar">
                          <IconButton size="small" color="primary" onClick={() => handleAbrirModalEditar(row)}>
                            <EditIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>

                        {row.estado === 'ACTIVO' ? (
                          <Tooltip title="Suspender">
                            <IconButton size="small" color="warning" onClick={() => handleCambiarEstado(row.id, 'SUSPENDIDO')}>
                              <PauseIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        ) : (
                          <Tooltip title="Activar">
                            <IconButton size="small" color="success" onClick={() => handleCambiarEstado(row.id, 'ACTIVO')}>
                              <CheckIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        )}

                        <Tooltip title="Eliminar">
                          <IconButton size="small" color="error" onClick={() => handleEliminar(row.id)}>
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </Stack>
                    </TableCell>
                  </TableRow>
                );
              })
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* MODAL DIÁLOGO (CREAR / EDITAR) */}
      <Dialog open={modalAbierto} onClose={() => setModalAbierto(false)} maxWidth="md" fullWidth>
        <DialogTitle sx={{ fontWeight: 700, pb: 1 }}>
          {modoEdicion ? 'Editar Registro de Correo' : 'Nuevo Correo Institucional'}
        </DialogTitle>
        <Divider />
        <Box component="form" onSubmit={handleGuardar}>
          <DialogContent sx={{ py: 2 }}>
            <Grid container spacing={2}>
              {/* SECCIÓN 1: DATOS DEL TITULAR */}
              <Grid item xs={12}><Typography variant="subtitle2" color="primary" sx={{ fontWeight: 700, mb: 1 }}>Datos del Titular</Typography></Grid>
              <Grid item xs={12} sm={4}><TextField fullWidth size="small" label="Nombre(s)" name="nombre" required value={formData.nombre} onChange={handleInputChange} /></Grid>
              <Grid item xs={12} sm={4}><TextField fullWidth size="small" label="Apellido Paterno" name="apellidoPaterno" required value={formData.apellidoPaterno} onChange={handleInputChange} /></Grid>
              <Grid item xs={12} sm={4}><TextField fullWidth size="small" label="Apellido Materno" name="apellidoMaterno" value={formData.apellidoMaterno} onChange={handleInputChange} /></Grid>
              <Grid item xs={12} sm={5}><TextField fullWidth size="small" label="Área / Adscripción" name="area" required placeholder="Ej. DIRECCIÓN DE INFORMÁTICA" value={formData.area} onChange={handleInputChange} /></Grid>
              <Grid item xs={12} sm={4}><TextField fullWidth size="small" label="Cargo" name="cargo" placeholder="Ej. ANALISTA A" value={formData.cargo} onChange={handleInputChange} /></Grid>
              <Grid item xs={12} sm={3}><TextField fullWidth size="small" label="Extensión" name="extension" placeholder="Ej. 104" value={formData.extension} onChange={handleInputChange} /></Grid>

              {/* SECCIÓN 2: CONFIGURACIÓN DE LA CUENTA */}
              <Grid item xs={12} sx={{ mt: 1 }}><Typography variant="subtitle2" color="primary" sx={{ fontWeight: 700, mb: 1 }}>Configuración de la Cuenta</Typography></Grid>
              <Grid item xs={12} sm={6}><TextField fullWidth size="small" type="email" label="Correo Institucional" name="correo" required placeholder="usuario@sedif.gob.mx" value={formData.correo} onChange={handleInputChange} /></Grid>
              <Grid item xs={12} sm={3}>
                <TextField select fullWidth size="small" label="Cuota Almacenamiento" name="cuotaAlmacenamiento" value={formData.cuotaAlmacenamiento} onChange={handleInputChange}>
                  <MenuItem value="2 GB">2 GB</MenuItem>
                  <MenuItem value="5 GB">5 GB</MenuItem>
                  <MenuItem value="10 GB">10 GB</MenuItem>
                  <MenuItem value="25 GB">25 GB</MenuItem>
                  <MenuItem value="ILIMITADO">ILIMITADO</MenuItem>
                </TextField>
              </Grid>
              <Grid item xs={12} sm={3}>
                <TextField select fullWidth size="small" label="Estado" name="estado" value={formData.estado} onChange={handleInputChange}>
                  <MenuItem value="ACTIVO">ACTIVO</MenuItem>
                  <MenuItem value="SUSPENDIDO">SUSPENDIDO</MenuItem>
                  <MenuItem value="BAJA">BAJA</MenuItem>
                  <MenuItem value="EN_PROCESO">EN PROCESO</MenuItem>
                </TextField>
              </Grid>
            </Grid>
          </DialogContent>
          <Divider />
          <DialogActions sx={{ p: 2 }}>
            <Button onClick={() => setModalAbierto(false)} color="inherit">Cancelar</Button>
            <Button type="submit" variant="contained" color="primary" sx={{ px: 3 }}>
              {modoEdicion ? 'Guardar Cambios' : 'Registrar Correo'}
            </Button>
          </DialogActions>
        </Box>
      </Dialog>

      {/* NOTIFICACIONES SNACKBAR */}
      <Snackbar open={snackbar.open} autoHideDuration={4000} onClose={handleCerrarSnackbar} anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}>
        <Alert onClose={handleCerrarSnackbar} severity={snackbar.tipo} variant="filled" sx={{ width: '100%' }}>
          {snackbar.mensaje}
        </Alert>
      </Snackbar>
    </Box>
  );
};