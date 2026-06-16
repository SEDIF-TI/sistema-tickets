import React, { useEffect, useState } from 'react';
import { Switch, Box, Typography, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, Button, TextField, Dialog, DialogActions, DialogContent, DialogTitle, Autocomplete } from '@mui/material';
import { areaService } from '../../services/areaService';
import { userService } from '../../services/userService'; // <-- Importamos para traer a los técnicos

const AdminAreasPage = () => {
    const [areas, setAreas] = useState([]);
    const [tecnicos, setTecnicos] = useState([]); // <-- Guardará solo a los usuarios de SOPORTE
    const [busqueda, setBusqueda] = useState('');
    
    // Estados para el Modal de Crear/Editar Área
    const [open, setOpen] = useState(false);
    const [currentArea, setCurrentArea] = useState({ nombre: '' });

    // Estados para el Modal de Asignar Soporte
    const [openSoporte, setOpenSoporte] = useState(false);
    const [areaSoporte, setAreaSoporte] = useState(null);
    const [tecnicoSeleccionado, setTecnicoSeleccionado] = useState(null);

    const cargarDatos = async () => {
        try {
            // Descargamos áreas y usuarios al mismo tiempo para mayor velocidad
            const [resAreas, resUsuarios] = await Promise.all([
                areaService.getAll(),
                userService.getAll()
            ]);

            // Ordenamos áreas
            const areasOrdenadas = resAreas.data.sort((a, b) => a.nombre.localeCompare(b.nombre));
            setAreas(areasOrdenadas);

            // Filtramos a los usuarios para quedarnos SOLO con los técnicos de soporte activos
            const soportesActivos = resUsuarios.data.filter(u => u.rolNombre === 'SOPORTE' && u.activo);
            setTecnicos(soportesActivos);
        } catch (error) {
            console.error("Error al cargar datos:", error);
        }
    };

    useEffect(() => { cargarDatos(); }, []);

    // --- Funciones del CRUD de Áreas ---
    const handleSaveArea = async () => {
        if (currentArea.id) await areaService.update(currentArea.id, currentArea);
        else await areaService.create(currentArea);
        setOpen(false);
        cargarDatos();
    };

    const handleTogglePrioridad = async (area) => {
        try {
            const payload = { ...area, prioritaria: !area.prioritaria };
            await areaService.update(area.id, payload);
            cargarDatos();
        } catch (error) {
            alert("Ocurrió un error al intentar cambiar la prioridad.");
        }
    };

    // --- Nueva Función: Guardar el Soporte Fijo ---
    const handleGuardarSoporte = async () => {
        if (!tecnicoSeleccionado) {
            alert("Por favor, selecciona un técnico.");
            return;
        }

        try {
            await areaService.asignarSoporteFijo(areaSoporte.id, tecnicoSeleccionado.id);
            alert("Soporte fijo asignado correctamente.");
            setOpenSoporte(false);
            setAreaSoporte(null);
            setTecnicoSeleccionado(null);
            cargarDatos();
        } catch (error) {
            const mensajeError = error.response?.data || "Ocurrió un error al asignar el soporte.";
            alert(`Error: ${mensajeError}`);
        }
    };

    const areasFiltradas = areas.filter(area => 
        area.nombre.toLowerCase().includes(busqueda.toLowerCase())
    );

    return (
        <Box sx={{ p: 3 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h5">Gestión de Áreas</Typography>
                <Button variant="contained" color="primary" onClick={() => { setCurrentArea({ nombre: '' }); setOpen(true); }}>
                    + Nueva Área
                </Button>
            </Box>

            <TextField
                label="Buscar área por nombre..."
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
                            <TableCell align="center">Prioritaria</TableCell>
                            <TableCell align="center">Soporte Asignado</TableCell>
                            <TableCell>Acciones</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {areasFiltradas.map(a => (
                            <TableRow key={a.id}>
                                <TableCell>{a.nombre}</TableCell>
                                <TableCell align="center">
                                    <Switch
                                        checked={a.prioritaria || false}
                                        onChange={() => handleTogglePrioridad(a)}
                                        color="warning"
                                    />
                                </TableCell>
                                {/* NUEVA COLUMNA: Muestra si ya tiene alguien asignado (opcional si tu backend lo devuelve) */}
                                <TableCell align="center">
                                    {a.soporteFijoNombre ? a.soporteFijoNombre : <Typography variant="caption" color="textSecondary">Sin asignar</Typography>}
                                </TableCell>
                                
                                <TableCell>
                                    {/* NUEVO BOTÓN PARA ASIGNAR */}
                                    <Button 
                                        size="small" 
                                        color="success" 
                                        onClick={() => { setAreaSoporte(a); setOpenSoporte(true); }}
                                    >
                                        Asignar Soporte
                                    </Button>
                                    <Button size="small" onClick={() => { setCurrentArea(a); setOpen(true); }}>Editar</Button>
                                    <Button size="small" color="error" onClick={async () => { await areaService.delete(a.id); cargarDatos(); }}>Eliminar</Button>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>

            {/* Modal para Crear/Editar Área */}
            <Dialog open={open} onClose={() => setOpen(false)}>
                <DialogTitle>{currentArea.id ? "Editar" : "Crear"} Área</DialogTitle>
                <DialogContent>
                    <TextField 
                        label="Nombre del Área" 
                        fullWidth 
                        margin="dense"
                        value={currentArea.nombre} 
                        onChange={e => setCurrentArea({...currentArea, nombre: e.target.value})} 
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setOpen(false)}>Cancelar</Button>
                    <Button onClick={handleSaveArea} variant="contained" color="primary">Guardar</Button>
                </DialogActions>
            </Dialog>

            {/* <-- NUEVO MODAL: ASIGNAR SOPORTE FIJO --> */}
            <Dialog open={openSoporte} onClose={() => setOpenSoporte(false)} maxWidth="xs" fullWidth>
                <DialogTitle>Asignar Soporte</DialogTitle>
                <DialogContent>
                    <Typography variant="body2" sx={{ mb: 2, mt: 1 }}>
                        Selecciona el técnico que será responsable de los tickets del área: <strong>{areaSoporte?.nombre}</strong>
                    </Typography>
                    
                    <Autocomplete
                        options={tecnicos}
                        getOptionLabel={(option) => `${option.nombre} (${option.correo})`}
                        value={tecnicoSeleccionado}
                        onChange={(event, newValue) => setTecnicoSeleccionado(newValue)}
                        renderInput={(params) => (
                            <TextField {...params} label="Técnico de Soporte" margin="dense" fullWidth />
                        )}
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => { setOpenSoporte(false); setTecnicoSeleccionado(null); }} color="inherit">
                        Cancelar
                    </Button>
                    <Button onClick={handleGuardarSoporte} variant="contained" color="success">
                        Asignar Técnico
                    </Button>
                </DialogActions>
            </Dialog>

        </Box>
    );
};

export default AdminAreasPage;