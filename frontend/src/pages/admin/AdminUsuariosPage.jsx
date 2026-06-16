import React, { useEffect, useState } from 'react';
import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, Button, Typography, Chip } from '@mui/material';
import { userService } from '../../services/userService';

const AdminUsuariosPage = () => {
    const [usuarios, setUsuarios] = useState([]);

    const cargarUsuarios = async () => {
        try {
            const response = await userService.getAll();
            setUsuarios(response.data);
        } catch (error) {
            console.error("Error al cargar usuarios", error);
        }
    };

    useEffect(() => { cargarUsuarios(); }, []);

    return (
        <TableContainer component={Paper} sx={{ m: 2, width: 'auto' }}>
            <Typography variant="h5" sx={{ p: 2 }}>Gestión de Usuarios</Typography>
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
                    {usuarios.map((u) => (
                        <TableRow key={u.id}>
                            <TableCell>{u.nombre}</TableCell>
                            <TableCell>{u.correo}</TableCell>
                            <TableCell>{u.rolNombre}</TableCell>
                            <TableCell>
                                {u.passwordTemporal && <Chip label="Clave Temporal" color="warning" size="small" />}
                                {!u.activo && <Chip label="Inactivo" color="error" size="small" />}
                            </TableCell>
                            <TableCell>
                                <Button onClick={() => userService.resetPassword(u.id).then(cargarUsuarios)}>Reset Clave</Button>
                            </TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </TableContainer>
    );
};

export default AdminUsuariosPage;