import React, { useEffect, useState } from 'react';
import { 
    Table, TableBody, TableCell, TableContainer, TableHead, TableRow, 
    Paper, Button, Typography, Chip, Dialog, DialogTitle, DialogContent, 
    DialogActions, TextField, Box, Autocomplete 
} from '@mui/material';
import { userService } from '../../services/userService';
import { areaService } from '../../services/areaService';

// IMPORTAMOS EL NUEVO MODAL (Ajusta la ruta si lo guardaste en otra carpeta)
import ModalCredenciales from '../../components/ModalCredenciales'; 

const rolesDisponibles = [
    { id: 4, nombre: 'ADMINISTRADOR' },
    { id: 5, nombre: 'SOPORTE' },
    { id: 6, nombre: 'EMPLEADO' }
];

const estadoInicial = { 
    id: null, 
    nombre: '', 
    correo: '', 
    username: '', 
    rolId: null, 
    areaId: null, 
    activo: true 
};

const AdminUsuariosPage = () => {
    const [usuarios, setUsuarios] = useState([]);
    const [areas, setAreas] = useState([]);
    const [busqueda, setBusqueda] = useState('');
    
    // Estados para los modales
    const [openModal, setOpenModal] = useState(false);
    const [formData, setFormData] = useState(estadoInicial);
    
    // Estados para el Modal de Credenciales
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
        setFormData({
            id: usuario.id,
            nombre: usuario.nombre,
            correo: usuario.correo,
            username: usuario.username,
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
                // Actualizar usuario existente
                await userService.update(formData.id, payload);
                alert("Usuario actualizado con éxito.");
                setOpenModal(false);
                setFormData(estadoInicial);
                cargarDatos();
            } else {
                // Crear usuario nuevo
                const response = await userService.create(payload);
                
                // Buscamos los nombres del Rol y Área para la credencial
                const nombreRol = rolesDisponibles.find(r => r.id === formData.rolId)?.nombre || '';
                const nombreArea = areas.find(a => a.id === formData.areaId)?.nombre || '';

                // Preparamos los datos para imprimir
                setDatosImpresion({
                    nombre: formData.nombre,
                    correo: formData.correo,
                    rol: nombreRol,
                    area: nombreArea,
                    password: response.data.passwordTemporalTexto // El dato que viene de Spring Boot
                });

                // Cerramos el formulario y abrimos el modal de éxito
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

    const usuariosFiltrados = usuarios.filter(u => 
        u.nombre.toLowerCase().includes(busqueda.toLowerCase()) ||
        u.correo.toLowerCase().includes(busqueda.toLowerCase()) ||
        u.rolNombre.toLowerCase().includes(busqueda.toLowerCase())
    );

    return (
        <Box sx={{ p: 3 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h5">Gestión de Usuarios</Typography>
                <Button variant="contained" color="primary" onClick={() => { setFormData(estadoInicial); setOpenModal(true); }}>
                    + Nuevo Usuario
                </Button>
            </Box>

            <TextField
                label="Buscar por nombre, correo o rol..."
                variant="outlined"
                size="small"
                fullWidth
                sx={{ mb: 2 }}
                value={busqueda}
                onChange={(e) => setBusqueda(e.target.value)}
            />

            <TableContainer component={Paper}>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell>Nombre</TableCell>
                            <TableCell>Correo</TableCell>
                            <TableCell>Rol</TableCell>
                            <TableCell>Estado</TableCell>
                            <TableCell>Acciones</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {usuariosFiltrados.map((u) => (
                            <TableRow key={u.id} sx={{ opacity: u.activo ? 1 : 0.6 }}>
                                <TableCell>{u.nombre}</TableCell>
                                <TableCell>{u.correo}</TableCell>
                                <TableCell>{u.rolNombre}</TableCell>
                                <TableCell>
                                    {u.passwordTemporal && <Chip label="Clave Temporal" color="warning" size="small" sx={{ mr: 1, mb: 1 }} />}
                                    {!u.activo ? <Chip label="Inactivo" color="error" size="small" /> : <Chip label="Activo" color="success" size="small" />}
                                </TableCell>
                                <TableCell>
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
                                <TableCell colSpan={5} align="center">No se encontraron usuarios.</TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
            </TableContainer>

            <Dialog open={openModal} onClose={() => setOpenModal(false)} maxWidth="sm" fullWidth>
                <DialogTitle>{formData.id ? "Editar Usuario" : "Registrar Nuevo Usuario"}</DialogTitle>
                <DialogContent>
                    <TextField margin="dense" label="Nombre Completo" name="nombre" fullWidth value={formData.nombre || ''} onChange={handleChange} />
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
                    <Button onClick={handleGuardar} variant="contained" color="primary">Guardar Usuario</Button>
                </DialogActions>
            </Dialog>

            {/* AQUI AGREGAMOS NUESTRO NUEVO COMPONENTE DE IMPRESIÓN */}
            <ModalCredenciales 
                open={openModalCredenciales} 
                onClose={() => setOpenModalCredenciales(false)} 
                usuarioData={datosImpresion} 
            />

        </Box>
    );
};

export default AdminUsuariosPage;