import React, { useState, useEffect } from 'react';
import {
  Box, Typography, Paper, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Button, Chip, Dialog, DialogTitle, DialogContent,
  DialogActions, TextField, MenuItem, Grid, IconButton, Tooltip, Alert,
  CircularProgress, Card, CardContent
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import AssignmentReturnIcon from '@mui/icons-material/AssignmentReturn';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import SearchIcon from '@mui/icons-material/Search';
import RefreshIcon from '@mui/icons-material/Refresh';
import InventoryIcon from '@mui/icons-material/Inventory';
import api from '../../services/api';

const COLOR_GUINDA = '#801A36';

export default function GestionResguardos() {
  const [resguardos, setResguardos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busqueda, setBusqueda] = useState('');
  const [openModal, setOpenModal] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  // Formulario alineado con el DTO/Entidad mapeado a campos con prefijo
  const initialFormState = {
    solicitanteNombre: '',  // s_solicitante_nombre
    solicitanteNumero: '',  // s_solicitante_numero
    departamento: '',       // s_departamento
    telefono: '',           // s_telefono
    equipoNombre: '',       // s_equipo_nombre
    numeroSerie: '',        // s_numero_serie
    numeroInventario: '',   // s_numero_inventario
    condiciones: 'BUENO',   // s_condiciones
    accesorios: '',         // s_accesorios
    duracionCantidad: 1,    // Para calcular d_fecha_vencimiento
    duracionTipo: 'Dias'   // Para calcular d_fecha_vencimiento
  };

  const [formData, setFormData] = useState(initialFormState);

  // Cargar resguardos desde el API
  const cargarResguardos = async () => {
    setLoading(true);
    try {
      const res = await api.get('/v1/resguardos');
      setResguardos(res.data || []);
      setErrorMsg('');
    } catch (err) {
      console.error('Error al cargar resguardos:', err);
      setErrorMsg('No se pudo obtener la lista de resguardos.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    cargarResguardos();
  }, []);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      await api.post('/v1/resguardos', formData);
      setOpenModal(false);
      setFormData(initialFormState);
      cargarResguardos();
    } catch (err) {
      console.error('Error creando resguardo:', err);
      alert('Error al expedir el resguardo. Revisa los datos.');
    }
  };

  const handleDevolucion = async (id) => {
    if (!window.confirm('¿Confirmas la recepción del equipo resguardado?')) return;
    try {
      await api.put(`/v1/resguardos/${id}/devolucion`);
      cargarResguardos();
    } catch (err) {
      console.error('Error en devolución:', err);
      alert('No se pudo marcar la devolución del resguardo.');
    }
  };

  const handleImprimirPdf = async (resguardo) => {
    try {
      const response = await api.post('/v1/documentos/resguardo', resguardo, {
        responseType: 'blob'
      });
      const blob = new Blob([response.data], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      window.open(url, '_blank');
    } catch (err) {
      console.error('Error al generar PDF:', err);
      alert('Error al generar la vista previa del PDF.');
    }
  };

  // Filtrado de registros en pantalla
  const resguardosFiltrados = resguardos.filter((r) => {
    const term = busqueda.toLowerCase();
    return (
      (r.solicitanteNombre && r.solicitanteNombre.toLowerCase().includes(term)) ||
      (r.solicitanteNumero && r.solicitanteNumero.toLowerCase().includes(term)) ||
      (r.equipoNombre && r.equipoNombre.toLowerCase().includes(term)) ||
      (r.numeroSerie && r.numeroSerie.toLowerCase().includes(term)) ||
      (r.numeroInventario && r.numeroInventario.toLowerCase().includes(term)) ||
      (r.departamento && r.departamento.toLowerCase().includes(term))
    );
  });

  const getChipColor = (estado) => {
    switch (estado) {
      case 'ENTREGADO':
      case 'ACTIVO':
        return 'success';
      case 'VENCIDO':
        return 'error';
      case 'DEVUELTO':
        return 'default';
      default:
        return 'primary';
    }
  };

  return (
    <Box sx={{ p: 1 }}>
      {/* HEADER */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <InventoryIcon sx={{ color: COLOR_GUINDA, fontSize: 32 }} />
          <Typography variant="h5" sx={{ fontWeight: 'bold', color: COLOR_GUINDA }}>
            Gestión y Control de Resguardos
          </Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setOpenModal(true)}
          sx={{ bgcolor: COLOR_GUINDA, '&:hover': { bgcolor: '#5f1328' } }}
        >
          Nuevo Resguardo
        </Button>
      </Box>

      {errorMsg && <Alert severity="error" sx={{ mb: 2 }}>{errorMsg}</Alert>}

      {/* BARRA DE BÚSQUEDA */}
      <Paper sx={{ p: 2, mb: 3, display: 'flex', gap: 2, alignItems: 'center' }}>
        <TextField
          fullWidth
          size="small"
          placeholder="Buscar por solicitante, No. empleado, equipo, número de serie o inventario..."
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
          InputProps={{ startAdornment: <SearchIcon sx={{ color: 'gray', mr: 1 }} /> }}
        />
        <Tooltip title="Actualizar lista">
          <IconButton onClick={cargarResguardos} color="primary">
            <RefreshIcon />
          </IconButton>
        </Tooltip>
      </Paper>

      {/* TABLA PRINCIPAL */}
      <TableContainer component={Paper} sx={{ boxShadow: 2, borderRadius: 1 }}>
        <Table sx={{ minWidth: 700 }}>
          <TableHead sx={{ bgcolor: COLOR_GUINDA }}>
            <TableRow>
              <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Solicitante</TableCell>
              <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Equipo / Serie / Inventario</TableCell>
              <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Ubicación / Teléfono</TableCell>
              <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Condición</TableCell>
              <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Estado</TableCell>
              <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Vencimiento</TableCell>
              <TableCell sx={{ color: 'white', fontWeight: 'bold', textAlign: 'center' }}>Acciones</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={7} align="center" sx={{ py: 3 }}>
                  <CircularProgress size={30} sx={{ color: COLOR_GUINDA }} />
                </TableCell>
              </TableRow>
            ) : resguardosFiltrados.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} align="center" sx={{ py: 3 }}>
                  No se encontraron resguardos registrados.
                </TableCell>
              </TableRow>
            ) : (
              resguardosFiltrados.map((r) => (
                <TableRow key={r.id} hover>
                  {/* s_solicitante_nombre & s_solicitante_numero */}
                  <TableCell>
                    <Typography variant="subtitle2" sx={{ fontWeight: 'bold' }}>
                      {r.solicitanteNombre}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      No. Empleado: {r.solicitanteNumero || 'N/A'}
                    </Typography>
                  </TableCell>

                  {/* s_equipo_nombre, s_numero_serie, s_numero_inventario */}
                  <TableCell>
                    <Typography variant="body2" sx={{ fontWeight: 'medium' }}>
                      {r.equipoNombre}
                    </Typography>
                    <Typography variant="caption" color="text.secondary" display="block">
                      S/N: {r.numeroSerie}
                    </Typography>
                    {r.numeroInventario && (
                      <Typography variant="caption" color="text.secondary">
                        Inv: {r.numeroInventario}
                      </Typography>
                    )}
                  </TableCell>

                  {/* s_departamento & s_telefono */}
                  <TableCell>
                    <Typography variant="caption" display="block">
                      Dep: {r.departamento || 'N/A'}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      Tel: {r.telefono || 'N/A'}
                    </Typography>
                  </TableCell>

                  {/* s_condiciones */}
                  <TableCell>
                    <Typography variant="caption" sx={{ fontWeight: 'bold', color: '#555' }}>
                      {r.condiciones || 'BUENO'}
                    </Typography>
                  </TableCell>

                  {/* s_estado_resguardo */}
                  <TableCell>
                    <Chip label={r.estado} color={getChipColor(r.estado)} size="small" />
                  </TableCell>

                  {/* d_fecha_vencimiento */}
                  <TableCell>
                    <Typography variant="body2">
                      {r.fechaVencimiento
                        ? new Date(r.fechaVencimiento).toLocaleDateString('es-MX')
                        : 'Indefinido'}
                    </Typography>
                  </TableCell>

                  {/* ACCIONES */}
                  <TableCell align="center">
                    <Box sx={{ display: 'flex', justifyContent: 'center', gap: 1 }}>
                      <Tooltip title="Descargar / Imprimir PDF">
                        <IconButton
                          size="small"
                          color="error"
                          onClick={() => handleImprimirPdf(r)}
                        >
                          <PictureAsPdfIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      {r.estado !== 'DEVUELTO' && (
                        <Tooltip title="Marcar como Devuelto">
                          <IconButton
                            size="small"
                            color="success"
                            onClick={() => handleDevolucion(r.id)}
                          >
                            <AssignmentReturnIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      )}
                    </Box>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* MODAL EXPEDIR RESGUARDO */}
      <Dialog open={openModal} onClose={() => setOpenModal(false)} maxWidth="md" fullWidth>
        <DialogTitle sx={{ bgcolor: COLOR_GUINDA, color: 'white', mb: 2 }}>
          Expedir Nuevo Resguardo
        </DialogTitle>
        <form onSubmit={handleSubmit}>
          <DialogContent>
            <Grid container spacing={2}>
              {/* DATOS SOLICITANTE */}
              <Grid item xs={12}>
                <Typography variant="subtitle2" sx={{ fontWeight: 'bold', color: COLOR_GUINDA }}>
                  1. Solicitante
                </Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth required size="small" label="Nombre del Solicitante (s_solicitante_nombre)"
                  name="solicitanteNombre" value={formData.solicitanteNombre} onChange={handleChange}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth required size="small" label="No. de Empleado (s_solicitante_numero)"
                  name="solicitanteNumero" value={formData.solicitanteNumero} onChange={handleChange}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth size="small" label="Departamento (s_departamento)"
                  name="departamento" value={formData.departamento} onChange={handleChange}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth size="small" label="Teléfono / Extensión (s_telefono)"
                  name="telefono" value={formData.telefono} onChange={handleChange}
                />
              </Grid>

              {/* DATOS EQUIPO */}
              <Grid item xs={12} sx={{ mt: 1 }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 'bold', color: COLOR_GUINDA }}>
                  2. Equipo
                </Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth required size="small" label="Equipo (s_equipo_nombre)"
                  name="equipoNombre" value={formData.equipoNombre} onChange={handleChange}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth required size="small" label="Número de Serie (s_numero_serie)"
                  name="numeroSerie" value={formData.numeroSerie} onChange={handleChange}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth size="small" label="Número de Inventario (s_numero_inventario)"
                  name="numeroInventario" value={formData.numeroInventario} onChange={handleChange}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  select fullWidth size="small" label="Condiciones (s_condiciones)"
                  name="condiciones" value={formData.condiciones} onChange={handleChange}
                >
                  <MenuItem value="EXCELENTE">Excelente</MenuItem>
                  <MenuItem value="BUENO">Bueno</MenuItem>
                  <MenuItem value="REGULAR">Regular</MenuItem>
                </TextField>
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth multiline rows={2} size="small" label="Accesorios (s_accesorios)"
                  name="accesorios" value={formData.accesorios} onChange={handleChange}
                />
              </Grid>

              {/* VIGENCIA */}
              <Grid item xs={12} sx={{ mt: 1 }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 'bold', color: COLOR_GUINDA }}>
                  3. Vigencia
                </Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth type="number" size="small" label="Duración (Cantidad)"
                  name="duracionCantidad" value={formData.duracionCantidad} onChange={handleChange}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  select fullWidth size="small" label="Tipo de Duración"
                  name="duracionTipo" value={formData.duracionTipo} onChange={handleChange}
                >
                  <MenuItem value="Dias">Días</MenuItem>
                  <MenuItem value="Semanas">Semanas</MenuItem>
                  <MenuItem value="Indefinido">Indefinido</MenuItem>
                </TextField>
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions sx={{ p: 2 }}>
            <Button onClick={() => setOpenModal(false)} color="inherit">
              Cancelar
            </Button>
            <Button type="submit" variant="contained" sx={{ bgcolor: COLOR_GUINDA }}>
              Guardar y Expedir
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
}