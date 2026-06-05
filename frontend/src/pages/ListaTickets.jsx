import { useEffect, useState } from 'react';
import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, Button, Typography } from '@mui/material';
import api from '../services/api';
import TicketModal from '../components/TicketModal';

export default function ListaTickets() {
    const [tickets, setTickets] = useState([]);
    const [openModal, setOpenModal] = useState(false);

    const fetchTickets = async () => {
        try {
            const response = await api.get('/v1/tickets');
            setTickets(response.data);
        } catch (error) {
            console.error("Error cargando tickets:", error);
        }
    };

    useEffect(() => { fetchTickets(); }, []);

    return (
        <Paper sx={{ p: 3, mt: 2 }}>
            <Typography variant="h5" sx={{ mb: 2 }}>Gestión de Tickets</Typography>
            <Button variant="contained" onClick={() => setOpenModal(true)} sx={{ mb: 2 }}>
                + Nuevo Ticket
            </Button>
            
            <TableContainer>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell>ID</TableCell>
                            <TableCell>Título</TableCell>
                            <TableCell>Prioridad</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {tickets.map((t) => (
                            <TableRow key={t.id}>
                                <TableCell>{t.id}</TableCell>
                                <TableCell>{t.titulo}</TableCell>
                                <TableCell>{t.prioridad}</TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>

            {/* El modal se renderiza aquí */}
            <TicketModal 
                open={openModal} 
                handleClose={() => setOpenModal(false)} 
                onTicketCreated={fetchTickets} 
            />
        </Paper>
    );
}