import { useEffect, useState } from 'react';
import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, Button } from '@mui/material';
import api from '../services/api';
import TicketModal from '../components/TicketModal';

export default function ListaTickets() {
    const [tickets, setTickets] = useState([]);
    const [open, setOpen] = useState(false);

    const fetchTickets = async () => {
        const response = await api.get('/v1/tickets');
        setTickets(response.data);
    };

    useEffect(() => { fetchTickets(); }, []);

    return (
        <Paper sx={{ p: 3 }}>
            <Button variant="contained" onClick={() => setOpen(true)} sx={{ mb: 2 }}>
                Crear Ticket
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
                        {tickets.map(t => (
                            <TableRow key={t.id}>
                                <TableCell>{t.id}</TableCell>
                                <TableCell>{t.titulo}</TableCell>
                                <TableCell>{t.prioridad}</TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
            <TicketModal open={open} handleClose={() => setOpen(false)} onTicketCreated={fetchTickets} />
        </Paper>
    );
}