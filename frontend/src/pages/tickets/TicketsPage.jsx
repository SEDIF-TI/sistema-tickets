import { useState, useEffect } from 'react';
import { Box, Button, Modal, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import FormularioTicket from '../empleado/FormularioTicket';
import api from '../../services/api';

export default function TicketsPage() {
    const [tickets, setTickets] = useState([]);
    const [openModal, setOpenModal] = useState(false);

    const cargarTickets = async () => {
        try {
            const response = await api.get('/v1/tickets');
            setTickets(response.data);
        } catch (error) {
            console.error("Error al cargar:", error);
        }
    };

    useEffect(() => { cargarTickets(); }, []);

    return (
        <Box sx={{ p: 3 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
                <Typography variant="h4">Mis Tickets</Typography>
                <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpenModal(true)}>Nuevo</Button>
            </Box>

            <TableContainer component={Paper}>
                <Table>
                    <TableHead>
                        <TableRow><TableCell>ID</TableCell><TableCell>Título</TableCell><TableCell>Estado</TableCell></TableRow>
                    </TableHead>
                    <TableBody>
                        {tickets.map((t) => (
                            <TableRow key={t.id}><TableCell>{t.id}</TableCell><TableCell>{t.titulo}</TableCell><TableCell>{t.estado}</TableCell></TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>

            <Modal open={openModal} onClose={() => setOpenModal(false)}>
                <Box sx={{ position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', width: 500 }}>
                    <FormularioTicket />
                </Box>
            </Modal>
        </Box>
    );
}