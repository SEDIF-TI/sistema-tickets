import React, { useState, useEffect } from 'react';
import {
  Box, Paper, Typography, Button, TextField, MenuItem, Table, TableBody,
  TableCell, TableContainer, TableHead, TableRow, Chip, IconButton, Tooltip,
  Dialog, DialogTitle, DialogContent, DialogActions, Grid, Alert, Snackbar,
  CircularProgress, InputAdornment, Card, CardContent, Divider, Stack
} from '@mui/material';
import {
  Add as AddIcon,
  Search as SearchIcon,
  Edit as EditIcon,
  Build as BuildIcon,
  CheckCircle as CheckCircleIcon,
  Refresh as RefreshIcon,
  Computer as ComputerIcon,
  Handyman as HandymanIcon,
  AssignmentTurnedIn as AssignmentTurnedInIcon,
  PendingActions as PendingActionsIcon
} from '@mui/icons-material';
import { tallerService } from '../../services/tallerService';

// Estado inicial del formulario vacío
const INITIAL_FORM_STATE = {
  id: null,
  solicitanteNombre: '',
  solicitanteNumero: '',
  departamento: '',
  equipoTipo: 'PC de Escritorio',
  marca: '',
  modelo: '',
  numeroSerie: '',
  numeroInventario: '',
  condicionRecepcion: '',
  accesorios: '',
  fallaReportada: '',
  diagnostico: '',
  solucion: '',
  estadoTaller: 'RECIBIDO',
  tecnicoAsignadoId: ''
};

// Catálogo de estados del ciclo de vida
const ESTADOS_TALLER = [
  'RECIBIDO',
  'EN_DIAGNOSTICO',
  'EN_REPARACION',
  'ESPERA_REFACCIONES',
  'DICTAMINADO',
  'LISTO_PARA_ENTREGA',
  'ENTREGADO'
];

export const GestionTaller = () => {
  const [equipos, setEquipos] = useState([]);
  const [loading, setLoading] = useState(false);
  const [filtroTexto, setFiltroTexto] = useState('');

  // Modal y Formulario
  const [modalAbierto, setModalAbierto] = useState(false);
  const [modoEdicion, setModoEdicion] = useState(false);
  const [formData, setFormData] = useState(INITIAL_FORM_STATE);

  // Snackbar para notificaciones
  const [snackbar, setSnackbar] = useState({ open: false, mensaje: '', tipo: 'info' });

  useEffect(() => {
    cargarEquipos();
  }, []);

  const cargarEquipos = async (texto = filtroTexto) => {
    setLoading(true);
    try {
      const data = await tallerService.obtenerEquipos(texto);
      setEquipos(data);
    } catch (err) {
      mostrarMensaje(err.message, 'error');
    } finally {
      setLoading(false);
    }
  };

  const mostrarMensaje = (mensaje, tipo = 'info') => {
    setSnackbar({ open: true, mensaje, tipo });
  };

  const handleBuscar = (e) => {
    e.preventDefault();
    cargarEquipos(filtroTexto);
  };

  const handleAbrirModalCrear = () => {
    setFormData(INITIAL_FORM_STATE);
    setModoEdicion(false);
    setModalAbierto(true);
  };

  const handleAbrirModalEditar = (item) => {
    setFormData({
      ...INITIAL_FORM_STATE,
      ...item,
      tecnicoAsignadoId: item.tecnicoAsignadoId || ''
    });
    setModoEdicion(true);
    setModalAbierto(true);
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleGuardar = async (e) => {
    e.preventDefault();
    try {
      if (modoEdicion) {
        await tallerService.actualizarEquipo(formData.id, formData, formData.tecnicoAsignadoId);
        mostrarMensaje('Registro actualizado correctamente', 'success');
      } else {
        await tallerService.registrarEquipo(formData, formData.tecnicoAsignadoId);
        mostrarMensaje('Equipo registrado e ingresado al taller', 'success');
      }
      setModalAbierto(false);
      cargarEquipos();
    } catch (err) {
      mostrarMensaje(err.message, 'error');
    }
  };

  const handleCambiarEstadoRápido = async (id, nuevoEstado) => {
    try {
      await tallerService.cambiarEstado(id, nuevoEstado);
      mostrarMensaje(`El estado del equipo cambió a ${nuevoEstado}`, 'success');
      cargarEquipos();
    } catch (err) {
      mostrarMensaje(err.message, 'error');
    }
  };

  // Función para determinar el color del badge según el estado
  const getChipColor = (estado) => {
    switch (estado) {
      case 'RECIBIDO': return { color: 'info', variant: 'outlined' };
      case 'EN_DIAGNOSTICO': return { color: 'secondary', variant: 'filled' };
      case 'EN_REPARACION': return { color: 'warning', variant: 'filled' };
      case 'ESPERA_REFACCIONES': return { color: 'error', variant: 'filled' };
      case 'DICTAMINADO': return { color: 'default', variant: 'filled' };
      case 'LISTO_PARA_ENTREGA': return { color: 'primary', variant: 'filled' };
      case 'ENTREGADO': return { color: 'success', variant: 'filled' };
      default: return { color: 'default', variant: 'outlined' };
    }
  };

  // Métricas rápidas
  const totalEnTaller = equipos.filter(e => e.estadoTaller !== 'ENTREGADO' && e.estadoTaller !== 'DICTAMINADO').length;
  const totalListos = equipos.filter(e => e.estadoTaller === 'LISTO_PARA_ENTREGA').length;

  return (
    <Box sx={{ p: { xs: 2, md: 3 } }}>
      {/* ENCABEZADO */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h5" sx={{ fontWeight: 700, color: 'text.primary' }}>
            Taller y Soporte Técnico
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Gestión de ingresos, diagnósticos y reparaciones de equipos
          </Typography>
        </Box>
        <Button
          variant="contained"
          color="primary"
          startIcon={<AddIcon />}
          onClick={handleAbrirModalCrear}
        >
          Nuevo Ingreso
        </Button>
      </Box>

      {/* MÉTRICAS */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={4}>
          <Card variant="outlined">
            <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <Box sx={{ p: 1.5, borderRadius: 2, bgcolor: 'primary.light', color: 'primary.main', display: 'flex' }}>
                <ComputerIcon />
              </Box>
              <Box>
                <Typography variant="body2" color="text.secondary">Total Registros Históricos</Typography>
                <Typography variant="h6" sx={{ fontWeight: 700 }}>{equipos.length}</Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Card variant="outlined">
            <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <Box sx={{ p: 1.5, borderRadius: 2, bgcolor: 'warning.light', color: 'warning.dark', display: 'flex' }}>
                <HandymanIcon />
              </Box>
              <Box>
                <Typography variant="body2" color="text.secondary">Equipos en Taller (Activos)</Typography>
                <Typography variant="h6" sx={{ fontWeight: 700, color: 'warning.dark' }}>{totalEnTaller}</Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Card variant="outlined">
            <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <Box sx={{ p: 1.5, borderRadius: 2, bgcolor: 'success.light', color: 'success.dark', display: 'flex' }}>
                <AssignmentTurnedInIcon />
              </Box>
              <Box>
                <Typography variant="body2" color="text.secondary">Listos para Entrega</Typography>
                <Typography variant="h6" sx={{ fontWeight: 700, color: 'success.main' }}>{totalListos}</Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* BÚSQUEDA */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <Box component="form" onSubmit={handleBuscar} sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
          <TextField
            fullWidth
            size="small"
            placeholder="Buscar por folio, serie, inventario o solicitante..."
            value={filtroTexto}
            onChange={(e) => setFiltroTexto(e.target.value)}
            InputProps={{
              startAdornment: <InputAdornment position="start"><SearchIcon color="action" /></InputAdornment>,
            }}
          />
          <Button variant="outlined" type="submit" startIcon={<SearchIcon />} sx={{ px: 3 }}>
            Buscar
          </Button>
          <IconButton onClick={() => cargarEquipos()} title="Recargar lista">
            <RefreshIcon />
          </IconButton>
        </Box>
      </Paper>

      {/* TABLA DE EQUIPOS */}
      <TableContainer component={Paper}>
        <Table>
          <TableHead sx={{ bgcolor: 'grey.100' }}>
            <TableRow>
              <TableCell sx={{ fontWeight: 700 }}>Folio</TableCell>
              <TableCell sx={{ fontWeight: 700 }}>Solicitante / Área</TableCell>
              <TableCell sx={{ fontWeight: 700 }}>Equipo</TableCell>
              <TableCell sx={{ fontWeight: 700 }}>Técnico Asignado</TableCell>
              <TableCell sx={{ fontWeight: 700 }}>Estado</TableCell>
              <TableCell align="right" sx={{ fontWeight: 700, pr: 3 }}>Acciones</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={6} align="center" sx={{ py: 5 }}>
                  <CircularProgress size={32} sx={{ mr: 1, verticalAlign: 'middle' }} />
                  Cargando información del taller...
                </TableCell>
              </TableRow>
            ) : equipos.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} align="center" sx={{ py: 5 }}>No se encontraron equipos en el taller.</TableCell>
              </TableRow>
            ) : (
              equipos.map((row) => {
                const chipProps = getChipColor(row.estadoTaller);
                return (
                  <TableRow key={row.id} hover>
                    <TableCell>
                      <Typography variant="subtitle2" color="primary.main">{row.folio}</Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" sx={{ fontWeight: 600 }}>{row.solicitanteNombre}</Typography>
                      <Typography variant="caption" color="text.secondary">{row.departamento}</Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2">{row.equipoTipo} {row.marca}</Typography>
                      <Typography variant="caption" color="text.secondary">S/N: {row.numeroSerie || 'N/A'}</Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2">{row.tecnicoAsignadoNombre || 'Sin asignar'}</Typography>
                    </TableCell>
                    <TableCell>
                      <Chip label={row.estadoTaller.replace(/_/g, ' ')} size="small" color={chipProps.color} variant={chipProps.variant} sx={{ fontWeight: 600, fontSize: '0.75rem' }} />
                    </TableCell>
                    <TableCell align="right" sx={{ pr: 2 }}>
                      <Stack direction="row" spacing={0.5} justifyContent="flex-end">
                        <Tooltip title="Actualizar / Diagnóstico">
                          <IconButton size="small" color="primary" onClick={() => handleAbrirModalEditar(row)}>
                            <EditIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        {row.estadoTaller === 'EN_DIAGNOSTICO' && (
                          <Tooltip title="Pasar a Reparación">
                            <IconButton size="small" color="warning" onClick={() => handleCambiarEstadoRápido(row.id, 'EN_REPARACION')}>
                              <BuildIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        )}
                        {row.estadoTaller === 'EN_REPARACION' && (
                          <Tooltip title="Marcar Listo para Entrega">
                            <IconButton size="small" color="success" onClick={() => handleCambiarEstadoRápido(row.id, 'LISTO_PARA_ENTREGA')}>
                              <PendingActionsIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        )}
                        {row.estadoTaller === 'LISTO_PARA_ENTREGA' && (
                          <Tooltip title="Marcar como Entregado">
                            <IconButton size="small" color="success" onClick={() => handleCambiarEstadoRápido(row.id, 'ENTREGADO')}>
                              <CheckCircleIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        )}
                      </Stack>
                    </TableCell>
                  </TableRow>
                );
              })
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* MODAL CREAR / EDITAR */}
      <Dialog open={modalAbierto} onClose={() => setModalAbierto(false)} maxWidth="md" fullWidth>
        <DialogTitle sx={{ fontWeight: 700, pb: 1 }}>
          {modoEdicion ? `Actualizar Equipo: ${formData.folio}` : 'Registrar Nuevo Ingreso a Taller'}
        </DialogTitle>
        <Divider />
        <Box component="form" onSubmit={handleGuardar}>
          <DialogContent sx={{ py: 2 }}>
            <Grid container spacing={2}>
              
              {/* DATOS DEL SOLICITANTE */}
              <Grid item xs={12}><Typography variant="subtitle2" color="primary" sx={{ fontWeight: 700 }}>Datos del Solicitante</Typography></Grid>
              <Grid item xs={12} sm={5}>
                <TextField fullWidth size="small" label="Nombre Completo" name="solicitanteNombre" required value={formData.solicitanteNombre} onChange={handleInputChange} />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField fullWidth size="small" label="Departamento / Área" name="departamento" required value={formData.departamento} onChange={handleInputChange} />
              </Grid>
              <Grid item xs={12} sm={3}>
                <TextField fullWidth size="small" label="Teléfono / Extensión" name="solicitanteNumero" value={formData.solicitanteNumero} onChange={handleInputChange} />
              </Grid>

              {/* DATOS DEL EQUIPO */}
              <Grid item xs={12} sx={{ mt: 1 }}><Typography variant="subtitle2" color="primary" sx={{ fontWeight: 700 }}>Identificación del Equipo</Typography></Grid>
              <Grid item xs={12} sm={4}>
                <TextField select fullWidth size="small" label="Tipo de Equipo" name="equipoTipo" required value={formData.equipoTipo} onChange={handleInputChange}>
                  <MenuItem value="PC de Escritorio">PC de Escritorio</MenuItem>
                  <MenuItem value="Laptop">Laptop</MenuItem>
                  <MenuItem value="Impresora">Impresora</MenuItem>
                  <MenuItem value="No Break / UPS">No Break / UPS</MenuItem>
                  <MenuItem value="Monitor">Monitor</MenuItem>
                  <MenuItem value="Otro">Otro</MenuItem>
                </TextField>
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField fullWidth size="small" label="Marca" name="marca" value={formData.marca} onChange={handleInputChange} />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField fullWidth size="small" label="Modelo" name="modelo" value={formData.modelo} onChange={handleInputChange} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth size="small" label="Número de Serie (S/N)" name="numeroSerie" value={formData.numeroSerie} onChange={handleInputChange} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth size="small" label="Número de Inventario" name="numeroInventario" value={formData.numeroInventario} onChange={handleInputChange} />
              </Grid>

              {/* RECEPCIÓN Y DIAGNÓSTICO */}
              <Grid item xs={12} sx={{ mt: 1 }}><Typography variant="subtitle2" color="primary" sx={{ fontWeight: 700 }}>Detalles del Servicio</Typography></Grid>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth size="small" label="Falla Reportada por Usuario" name="fallaReportada" multiline rows={2} required value={formData.fallaReportada} onChange={handleInputChange} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth size="small" label="Condiciones de Recepción / Accesorios" name="condicionRecepcion" multiline rows={2} placeholder="Rayones, sin cargador, cable dañado..." value={formData.condicionRecepcion} onChange={handleInputChange} />
              </Grid>
              
              {modoEdicion && (
                <>
                  <Grid item xs={12} sm={6}>
                    <TextField fullWidth size="small" label="Diagnóstico Técnico Real" name="diagnostico" multiline rows={3} value={formData.diagnostico || ''} onChange={handleInputChange} />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField fullWidth size="small" label="Solución Aplicada / Refacciones" name="solucion" multiline rows={3} value={formData.solucion || ''} onChange={handleInputChange} />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField select fullWidth size="small" label="Estado Actual en Taller" name="estadoTaller" value={formData.estadoTaller} onChange={handleInputChange}>
                      {ESTADOS_TALLER.map(estado => (
                        <MenuItem key={estado} value={estado}>{estado.replace(/_/g, ' ')}</MenuItem>
                      ))}
                    </TextField>
                  </Grid>
                </>
              )}
            </Grid>
          </DialogContent>
          <Divider />
          <DialogActions sx={{ p: 2 }}>
            <Button onClick={() => setModalAbierto(false)} color="inherit">Cancelar</Button>
            <Button type="submit" variant="contained" color="primary" sx={{ px: 3 }}>
              {modoEdicion ? 'Guardar Cambios' : 'Ingresar Equipo'}
            </Button>
          </DialogActions>
        </Box>
      </Dialog>

      <Snackbar open={snackbar.open} autoHideDuration={4000} onClose={() => setSnackbar({ ...snackbar, open: false })} anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}>
        <Alert onClose={() => setSnackbar({ ...snackbar, open: false })} severity={snackbar.tipo} variant="filled" sx={{ width: '100%' }}>
          {snackbar.mensaje}
        </Alert>
      </Snackbar>
    </Box>
  );
};