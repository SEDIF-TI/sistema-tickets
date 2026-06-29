import React, { useEffect, useState } from 'react';
import { 
    Box, Typography, CircularProgress, Alert, Table, TableBody, TableCell, TableContainer, TableRow, Button
} from '@mui/material';
import { 
    BarChart, Bar, LineChart, Line, PieChart, Pie, Cell, 
    XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, LabelList
} from 'recharts';
import api from '../../services/api';

// Iconos
import HeadsetMicIcon from '@mui/icons-material/HeadsetMic';
import AssignmentIcon from '@mui/icons-material/Assignment';
import GroupIcon from '@mui/icons-material/Group';
import ListAltIcon from '@mui/icons-material/ListAlt';
import LocalOfferIcon from '@mui/icons-material/LocalOffer';
import CalendarMonthIcon from '@mui/icons-material/CalendarMonth';
import EngineeringIcon from '@mui/icons-material/Engineering';
import DomainIcon from '@mui/icons-material/Domain';
import CampaignIcon from '@mui/icons-material/Campaign'; // <-- Nuevo icono para Avisos

const COLOR_GUINDA = '#801A36'; 
const COLORS_PIE = ['#2ecc71', '#e74c3c', '#3498db', '#f1c40f', '#9b59b6'];

const MockupCard = ({ titulo, icono: Icono, children }) => (
    <Box sx={{ 
        bgcolor: 'white', borderRadius: 1, overflow: 'hidden', border: '1px solid #e0e0e0',
        height: '100%', display: 'flex', flexDirection: 'column', boxShadow: '0px 2px 4px rgba(0,0,0,0.05)'
    }}>
        <Box sx={{ bgcolor: COLOR_GUINDA, color: 'white', py: 1, px: 2, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 1 }}>
            {Icono && <Icono fontSize="small" />}
            <Typography variant="subtitle2" sx={{ fontSize: '0.85rem' }}>{titulo}</Typography>
        </Box>
        <Box sx={{ p: 2, flexGrow: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
            {children}
        </Box>
    </Box>
);

export default function DashboardPage() {
    const [data, setData] = useState(null);
    const [error, setError] = useState('');

    useEffect(() => {
        api.get('/v1/admin/dashboard/metricas')
            .then(res => setData(res.data))
            .catch(err => setError("Error al cargar las métricas."));
    }, []);

    if (error) return <Alert severity="error" sx={{ m: 3 }}>{error}</Alert>;
    if (!data) return (
        <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80vh' }}>
            <CircularProgress sx={{ color: COLOR_GUINDA }} />
        </Box>
    );

    return (
        <Box sx={{ p: 3, backgroundColor: '#f0f2f5', minHeight: '100vh' }}>
            <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(12, 1fr)', gap: 3 }}>

                <Box sx={{ gridColumn: 'span 12', bgcolor: COLOR_GUINDA, color: 'white', p: 3, borderRadius: 1, textAlign: 'center' }}>
                    <Typography variant="h5" fontWeight="bold" sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 1 }}>
                        <HeadsetMicIcon /> Bienvenido al Sistema de Soporte Técnico
                    </Typography>
                    <Typography variant="body2" sx={{ mt: 1, opacity: 0.9 }}>
                        Aquí podrás gestionar tickets, revisar la bitácora y administrar usuarios del sistema.
                    </Typography>
                </Box>

                {/* NUEVA GRÁFICA DE AVISOS (Ocupa 12 columnas arriba de los estatus) */}
                <Box sx={{ gridColumn: 'span 12' }}>
                    <MockupCard titulo="Alcance de Avisos Activos" icono={CampaignIcon}>
                        <ResponsiveContainer width="100%" height={250}>
                            <BarChart data={data.avisosPorArea || []} margin={{ top: 20, right: 30, left: 0, bottom: 5 }} layout="vertical">
                                <CartesianGrid strokeDasharray="3 3" horizontal={true} vertical={false} />
                                <XAxis type="number" />
                                <YAxis dataKey="nombre" type="category" width={150} tick={{fontSize: 12}} />
                                <Tooltip cursor={{fill: '#f5f5f5'}} />
                                <Bar dataKey="cantidad" name="Avisos Activos" fill="#f1c40f" barSize={30} radius={[0, 4, 4, 0]}>
                                    <LabelList dataKey="cantidad" position="right" style={{ fontSize: '12px', fontWeight: 'bold' }} />
                                </Bar>
                            </BarChart>
                        </ResponsiveContainer>
                    </MockupCard>
                </Box>

                {/* TÍTULOS POR ESTATUS */}
                <Box sx={{ gridColumn: 'span 12' }}>
                    <MockupCard titulo="Tickets por Estatus" icono={ListAltIcon}>
                        <Box sx={{ display: 'flex', flexDirection: { xs: 'column', md: 'row' }, alignItems: 'center' }}>
                            <Box sx={{ width: { xs: '100%', md: '50%' }, height: 300 }}>
                                <ResponsiveContainer width="100%" height="100%">
                                    <PieChart>
                                        <Pie data={data.porEstatus || []} innerRadius={70} outerRadius={110} dataKey="cantidad" nameKey="nombre">
                                            {(data.porEstatus || []).map((_, i) => <Cell key={i} fill={COLORS_PIE[i % COLORS_PIE.length]} />)}
                                        </Pie>
                                        <Tooltip />
                                    </PieChart>
                                </ResponsiveContainer>
                            </Box>
                            <Box sx={{ width: { xs: '100%', md: '50%' }, px: 2 }}>
                                <TableContainer>
                                    <Table size="small">
                                        <TableBody>
                                            {(data.porEstatus || []).map((item, i) => (
                                                <TableRow key={i}>
                                                    <TableCell sx={{ color: COLORS_PIE[i % COLORS_PIE.length], fontSize: '1.2rem', p: 1, borderBottom: 'none' }}>●</TableCell>
                                                    <TableCell sx={{ p: 1, borderBottom: '1px solid #f0f0f0' }}>{item.nombre}</TableCell>
                                                    <TableCell align="right" sx={{ fontWeight: 'bold', p: 1, borderBottom: '1px solid #f0f0f0' }}>{item.cantidad}</TableCell>
                                                </TableRow>
                                            ))}
                                        </TableBody>
                                    </Table>
                                </TableContainer>
                            </Box>
                        </Box>
                    </MockupCard>
                </Box>

                {/* CATEGORÍA Y FECHAS */}
                <Box sx={{ gridColumn: { xs: 'span 12', md: 'span 6' } }}>
                    <MockupCard titulo="Tickets por Categoría" icono={LocalOfferIcon}>
                        <ResponsiveContainer width="100%" height={350}>
                            <PieChart>
                                <Pie data={data.porPrioridad || []} outerRadius={120} dataKey="cantidad" nameKey="nombre">
                                    {(data.porPrioridad || []).map((_, i) => <Cell key={i} fill={COLORS_PIE[i % COLORS_PIE.length]} />)}
                                </Pie>
                                <Tooltip />
                                <Legend layout="vertical" verticalAlign="middle" align="right" wrapperStyle={{ fontSize: '11px', width: '40%' }} />
                            </PieChart>
                        </ResponsiveContainer>
                    </MockupCard>
                </Box>
                <Box sx={{ gridColumn: { xs: 'span 12', md: 'span 6' } }}>
                    <MockupCard titulo="Tickets por fecha" icono={CalendarMonthIcon}>
                        <ResponsiveContainer width="100%" height={350}>
                            <BarChart data={data.porFecha || []} margin={{ top: 20, right: 10, left: -20, bottom: 5 }}>
                                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                                <XAxis dataKey="fecha" tick={{fontSize: 10}} />
                                <YAxis tick={{fontSize: 10}} />
                                <Tooltip cursor={{fill: '#f5f5f5'}} />
                                <Bar dataKey="total" name="Tickets" fill="#5cb85c" barSize={40}>
                                    <LabelList dataKey="total" position="top" style={{ fontSize: '10px', fontWeight: 'bold' }} />
                                </Bar>
                            </BarChart>
                        </ResponsiveContainer>
                    </MockupCard>
                </Box>

                {/* INGENIEROS */}
                <Box sx={{ gridColumn: 'span 12' }}>
                    <MockupCard titulo="Tickets realizados por Ingeniero" icono={EngineeringIcon}>
                        <ResponsiveContainer width="100%" height={300}>
                            <LineChart data={data.porIngeniero || []} margin={{ top: 30, right: 30, left: 0, bottom: 10 }}>
                                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                                <XAxis dataKey="nombre" tick={{fontSize: 11}} />
                                <YAxis tick={{fontSize: 11}} />
                                <Tooltip />
                                <Legend verticalAlign="top" height={36} />
                                <Line type="linear" dataKey="cantidad" name="Tickets atendidos" stroke="#0275d8" strokeWidth={2} activeDot={{ r: 8 }}>
                                    <LabelList dataKey="cantidad" position="top" offset={10} style={{ fontSize: '11px', fontWeight: 'bold' }} />
                                </Line>
                            </LineChart>
                        </ResponsiveContainer>
                    </MockupCard>
                </Box>

                {/* DEPARTAMENTOS */}
                <Box sx={{ gridColumn: 'span 12' }}>
                    <MockupCard titulo="Tickets por departamento" icono={DomainIcon}>
                        <ResponsiveContainer width="100%" height={350}>
                            <BarChart data={data.porArea || []} margin={{ top: 30, right: 10, left: 0, bottom: 60 }}>
                                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                                <XAxis dataKey="nombre" angle={-45} textAnchor="end" interval={0} tick={{fontSize: 9}} />
                                <YAxis tick={{fontSize: 11}} />
                                <Tooltip cursor={{fill: '#f5f5f5'}} />
                                <Legend verticalAlign="top" height={36} />
                                <Bar dataKey="cantidad" name="Tickets realizados" fill="#17a2b8" barSize={40}>
                                    <LabelList dataKey="cantidad" position="top" style={{ fontSize: '11px', fontWeight: 'bold' }} />
                                </Bar>
                            </BarChart>
                        </ResponsiveContainer>
                    </MockupCard>
                </Box>

            </Box>
        </Box>
    );
}