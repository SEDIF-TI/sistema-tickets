import React, { useEffect, useState } from 'react';
import { 
    Table, TableBody, TableCell, TableContainer, TableHead, TableRow, 
    Paper, Button, Typography, Chip, Dialog, DialogTitle, DialogContent, 
    DialogActions, TextField, Box, Autocomplete, Tabs, Tab, Switch, Tooltip, Grid
} from '@mui/material';
import { userService } from '../../services/userService';
import { areaService } from '../../services/areaService';
import api from '../../services/api'; 

import ModalCredenciales from '../../components/ModalCredenciales'; 

const rolesDisponibles = [
    { id: 4, nombre: 'ADMINISTRADOR' },
    { id: 5, nombre: 'SOPORTE' },
    { id: 6, nombre: 'EMPLEADO' }
];

// ---> CAMBIO 1: Se agregan los apellidos al estado inicial
const estadoInicial = { 
    id: null, 
    nombre: '', 
    apellidoPaterno: '',
    apellidoMaterno: '',
    correo: '', 
    username: '', 
    rolId: null, 
    areaId: null, 
    activo: true 
};

const COLOR_GUINDA = '#801A36';

const AdminUsuariosPage = () => {
    const [usuarios, setUsuarios] = useState([]);
    const [areas, setAreas] = useState([]);
    const [busqueda, setBusqueda] = useState('');
    
    const [tabIndex, setTabIndex] = useState(0);
    
    const [openModal, setOpenModal] = useState(false);
    const [formData, setFormData] = useState(estadoInicial);
    
    const [openModalCredenciales, setOpenModalCredenciales] = useState(false);
    const [datosImpresion, setDatosImpresion] = useState(null);

    const cargarDatos = async () => {
        try {
            const [resUsuarios, resAreas] = await Promise.all([
                userService.getAll(),
                areaService.getAll()
            ]);
            setUsuarios(resUsuarios.data);
            
            const areasOrdenadas = resAreas.data.sort((a, b) => a.nombre.localeCompare(b.nombre));
            setAreas(areasOrdenadas);
        } catch (error) {
            console.error("Error al cargar datos", error);
        }
    };

    useEffect(() => { cargarDatos(); }, []);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData({ ...formData, [name]: value });
    };

    const handleEditar = (usuario) => {
        // ---> CAMBIO 2: Cargar los apellidos cuando se edita un usuario
        setFormData({
            id: usuario.id,
            nombre: usuario.nombre || '',
            apellidoPaterno: usuario.apellidoPaterno || '',
            apellidoMaterno: usuario.apellidoMaterno || '',
            correo: usuario.correo || '',
            username: usuario.username || '',
            rolId: usuario.rolId || null, 
            areaId: usuario.areaId || null,
            activo: usuario.activo
        });
        setOpenModal(true);
    };

    const handleGuardar = async () => {
        if (!formData.rolId || !formData.areaId) {
            alert("Por favor, selecciona tanto el Rol como el Área.");
            return;
        }

        try {
            const payload = {
                ...formData,
                areaId: Number(formData.areaId),
                rolId: Number(formData.rolId),
                disponibleSoporte: false 
            };

            if (formData.id) {
                await userService.update(formData.id, payload);
                alert("Usuario actualizado con éxito.");
                setOpenModal(false);
                setFormData(estadoInicial);
                cargarDatos();
            } else {
                const response = await userService.create(payload);
                
                const nombreRol = rolesDisponibles.find(r => r.id === formData.rolId)?.nombre || '';
                const nombreArea = areas.find(a => a.id === formData.areaId)?.nombre || '';

                // ---> CAMBIO 3: Unimos el nombre para que la credencial se imprima bonita
                const nombreCompleto = `${formData.nombre} ${formData.apellidoPaterno} ${formData.apellidoMaterno}`.trim();

                setDatosImpresion({
                    nombre: nombreCompleto,
                    correo: formData.correo,
                    rol: nombreRol,
                    area: nombreArea,
                    password: response.data.passwordTemporalTexto
                });

                setOpenModal(false);
                setFormData(estadoInicial);
                cargarDatos(); 
                setOpenModalCredenciales(true);
            }
            
        } catch (error) {
            const mensajeBackend = (error.response && typeof error.response.data === 'string') 
                ? error.response.data 
                : "Ocurrió un error en el servidor.";
            alert(`ATENCIÓN: ${mensajeBackend}`);
        }
    };

    const handleToggleActivo = async (usuario) => {
        const accion = usuario.activo ? 'dar de baja' : 'reactivar';
        if (window.confirm(`¿Estás seguro de que deseas ${accion} al usuario ${usuario.nombre}?`)) {
            try {
                const payload = {
                    ...usuario,
                    activo: !usuario.activo
                };
                await userService.update(usuario.id, payload);
                cargarDatos();
            } catch (error) {
                alert("Ocurrió un error al cambiar el estado del usuario.");
            }
        }
    };

    const handleToggleDisponibilidad = async (usuario, isChecked) => {
        try {
            await api.patch(`/v1/admin/usuarios/${usuario.id}/disponibilidad`, {
                disponibleSoporte: isChecked 
            });
            
            setUsuarios(prev => prev.map(u => 
                u.id === usuario.id ? { ...u, disponibleSoporte: isChecked } : u
            ));
        } catch (error) {
            console.error(error);
            alert("Error al actualizar la disponibilidad del técnico.");
        }
    };

    const usuariosFiltrados = usuarios.filter(u => {
        const pasaBusqueda = u.nombre.toLowerCase().includes(busqueda.toLowerCase()) ||
                             u.correo.toLowerCase().includes(busqueda.toLowerCase()) ||
                             u.rolNombre.toLowerCase().includes(busqueda.toLowerCase());
        
        if (!pasaBusqueda) return false;

        if (tabIndex === 1) return u.rolNombre === 'SOPORTE';
        if (tabIndex === 2) return u.rolNombre !== 'SOPORTE'; 
        
        return true; 
    });

    return (
        <Box sx={{ p: 3, maxWidth: 1300, mx: 'auto' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                <Typography variant="h5" fontWeight="bold" color={COLOR_GUINDA}>
                    Gestión de Usuarios
                </Typography>
                <Button variant="contained" onClick={() => { setFormData(estadoInicial); setOpenModal(true); }} sx={{ bgcolor: COLOR_GUINDA }}>
                    + Nuevo Usuario
                </Button>
            </Box>

            <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
                <Tabs 
                    value={tabIndex} 
                    onChange={(e, newValue) => setTabIndex(newValue)} 
                    textColor="inherit"
                    TabIndicatorProps={{ style: { backgroundColor: COLOR_GUINDA } }}
                >
                    <Tab label="Todos los Usuarios" />
                    <Tab label="Personal de Soporte" />
                    <Tab label="Resto del Personal" />
                </Tabs>
            </Box>

            <TextField
                label="Buscar por nombre, correo o rol..."
                variant="outlined"
                size="small"
                fullWidth
                sx={{ mb: 3 }}
                value={busqueda}
                onChange={(e) => setBusqueda(e.target.value)}
            />

            <TableContainer component={Paper} elevation={2}>
                <Table sx={{ minWidth: 900 }}>
                    <TableHead sx={{ bgcolor: '#f8fafc' }}>
                        <TableRow>
                            <TableCell sx={{ fontWeight: 'bold' }}>Nombre</TableCell>
                            <TableCell sx={{ fontWeight: 'bold' }}>Correo</TableCell>
                            <TableCell sx={{ fontWeight: 'bold' }}>Rol</TableCell>
                            <TableCell sx={{ fontWeight: 'bold', textAlign: 'center' }}>Asignación de Tickets</TableCell>
                            <TableCell sx={{ fontWeight: 'bold' }}>Estado</TableCell>
                            <TableCell sx={{ fontWeight: 'bold', textAlign: 'center' }}>Acciones</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {usuariosFiltrados.map((u) => (
                            <TableRow key={u.id} sx={{ opacity: u.activo ? 1 : 0.6 }}>
                                <TableCell>{u.nombre}</TableCell>
                                <TableCell>{u.correo}</TableCell>
                                <TableCell>{u.rolNombre}</TableCell>
                                
                                <TableCell align="center">
                                    {u.rolNombre === 'SOPORTE' ? (
                                        <Tooltip title={u.disponibleSoporte ? "Recibiendo tickets automáticamente" : "Ignorado por el balanceador"}>
                                            <Switch 
                                                checked={u.disponibleSoporte || false}
                                                onChange={(e) => handleToggleDisponibilidad(u, e.target.checked)}
                                                color="success"
                                                disabled={!u.activo} 
                                            />
                                        </Tooltip>
                                    ) : (
                                        <Typography variant="body2" color="textSecondary">--</Typography>
                                    )}
                                </TableCell>

                                <TableCell>
                                    {u.passwordTemporal && <Chip label="Clave Temporal" color="warning" size="small" sx={{ mr: 1, mb: 1 }} />}
                                    {!u.activo ? <Chip label="Inactivo" color="error" size="small" /> : <Chip label="Activo" color="success" size="small" />}
                                </TableCell>
                                
                                <TableCell align="center">
                                    <Button size="small" onClick={() => handleEditar(u)}>Editar</Button>
                                    <Button size="small" color={u.activo ? "error" : "success"} onClick={() => handleToggleActivo(u)}>
                                        {u.activo ? "Baja" : "Reactivar"}
                                    </Button>
                                    <Button size="small" onClick={() => userService.resetPassword(u.id).then(cargarDatos)}>
                                        Reset Clave
                                    </Button>
                                </TableCell>
                            </TableRow>
                        ))}
                        {usuariosFiltrados.length === 0 && (
                            <TableRow>
                                <TableCell colSpan={6} align="center" sx={{ py: 4 }}>
                                    <Typography color="textSecondary">No se encontraron usuarios en esta categoría.</Typography>
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
            </TableContainer>

            <Dialog open={openModal} onClose={() => setOpenModal(false)} maxWidth="sm" fullWidth>
                <DialogTitle>{formData.id ? "Editar Usuario" : "Registrar Nuevo Usuario"}</DialogTitle>
                <DialogContent>
                    
                    {/* ---> CAMBIO 4: Reemplazamos el TextField único por un Grid de 3 campos */}
                    <Grid container spacing={2} sx={{ mt: 0.5, mb: 1 }}>
                        <Grid item xs={12}>
                            <TextField 
                                fullWidth required 
                                label="Nombre(s)" 
                                name="nombre" 
                                value={formData.nombre} 
                                onChange={handleChange} 
                            />
                        </Grid>
                        <Grid item xs={12} sm={6}>
                            <TextField 
                                fullWidth required 
                                label="Apellido Paterno" 
                                name="apellidoPaterno" 
                                value={formData.apellidoPaterno} 
                                onChange={handleChange} 
                            />
                        </Grid>
                        <Grid item xs={12} sm={6}>
                            <TextField 
                                fullWidth 
                                label="Apellido Materno" 
                                name="apellidoMaterno" 
                                value={formData.apellidoMaterno} 
                                onChange={handleChange} 
                            />
                        </Grid>
                    </Grid>

                    <TextField margin="dense" label="Correo Electrónico" name="correo" type="email" fullWidth value={formData.correo || ''} onChange={handleChange} />
                    <TextField margin="dense" label="Nombre de Usuario (Login)" name="username" fullWidth value={formData.username || ''} onChange={handleChange} disabled={!!formData.id} />
                    
                    <Autocomplete
                        options={rolesDisponibles}
                        getOptionLabel={(option) => option.nombre}
                        value={rolesDisponibles.find(r => r.id === formData.rolId) || null}
                        onChange={(event, newValue) => {
                            setFormData({ ...formData, rolId: newValue ? newValue.id : null });
                        }}
                        renderInput={(params) => (
                            <TextField {...params} label="Rol del Usuario" margin="dense" fullWidth />
                        )}
                        sx={{ mt: 1 }}
                    />
                    
                    <Autocomplete
                        options={areas}
                        getOptionLabel={(option) => option.nombre}
                        value={areas.find(a => a.id === formData.areaId) || null}
                        onChange={(event, newValue) => {
                            setFormData({ ...formData, areaId: newValue ? newValue.id : null });
                        }}
                        renderInput={(params) => (
                            <TextField {...params} label="Área Asignada" margin="dense" fullWidth />
                        )}
                        sx={{ mt: 1 }}
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setOpenModal(false)} color="inherit">Cancelar</Button>
                    <Button onClick={handleGuardar} variant="contained" sx={{ bgcolor: COLOR_GUINDA }}>Guardar Usuario</Button>
                </DialogActions>
            </Dialog>

            <ModalCredenciales 
                open={openModalCredenciales} 
                onClose={() => setOpenModalCredenciales(false)} 
                usuarioData={datosImpresion} 
            />

        </Box>
    );
};

export default AdminUsuariosPage;