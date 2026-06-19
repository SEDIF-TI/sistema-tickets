import React, { useEffect, useState } from 'react';
import { Box, Grid, Card, CardContent, Typography, Paper, CircularProgress, Alert } from '@mui/material';
import { 
    BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip as ChartTooltip, Legend, ResponsiveContainer,
    PieChart, Pie, Cell
} from 'recharts';
import ConfirmationNumberIcon from '@mui/icons-material/ConfirmationNumber';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import PendingActionsIcon from '@mui/icons-material/PendingActions';
import api from '../../services/api'; // Importamos tu instancia configurada de Axios

const COLORES = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#AF19FF'];

export default function DashboardPage() {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [metricas, setMetricas] = useState(null);

    useEffect(() => {
        const cargarMetricas = async () => {
            try {
                setLoading(true);
                // Consumimos el endpoint del nuevo DashboardResource
                const respuesta = await api.get('/v1/admin/dashboard/metricas');
                setMetricas(respuesta.data);
                setError('');
            } catch (err) {
                console.error("Error al traer métricas:", err);
                setError('No se pudieron cargar las métricas del servidor.');
            } finally {
                setLoading(false);
            }
        };

        cargarMetricas();
    }, []);

    // Pantalla de carga integrada con Material UI
    if (loading) {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '70vh', flexDirection: 'column', gap: 2 }}>
                <CircularProgress size={60} />
                <Typography color="textSecondary">Cargando métricas en tiempo real...</Typography>
            </Box>
        );
    }

    if (error) {
        return <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>;
    }

    // Si por alguna razón viene vacío, evitamos colapsos de renderizado
    if (!metricas) return null;

    // Adaptamos los datos dinámicos para la gráfica de barras mensual
    // Nota: Como el backend actual devuelve totales, usamos los datos actuales para el mes en curso
    const datosMensuales = [
        { mes: 'Actual', resueltos: metricas.resueltos, pendientes: metricas.pendientes }
    ];

    return (
        <Box sx={{ p: 1 }}>
            <Typography variant="h4" sx={{ mb: 4, fontWeight: 'bold', color: '#2c3e50' }}>
                Panel de Jefatura - Métricas Reales
            </Typography>

            {/* TARJETAS DE RESUMEN DINÁMICAS */}
            <Grid container spacing={3} sx={{ mb: 4 }}>
                <Grid item xs={12} sm={4}>
                    <Card elevation={3} sx={{ borderLeft: '5px solid #2196f3' }}>
                        <CardContent sx={{ display: 'flex', alignItems: 'center' }}>
                            <ConfirmationNumberIcon sx={{ fontSize: 50, color: '#2196f3', mr: 2 }} />
                            <Box>
                                <Typography color="textSecondary" variant="h6">Total Tickets</Typography>
                                <Typography variant="h3" fontWeight="bold">{metricas.totalTickets}</Typography>
                            </Box>
                        </CardContent>
                    </Card>
                </Grid>
                <Grid item xs={12} sm={4}>
                    <Card elevation={3} sx={{ borderLeft: '5px solid #4caf50' }}>
                        <CardContent sx={{ display: 'flex', alignItems: 'center' }}>
                            <CheckCircleIcon sx={{ fontSize: 50, color: '#4caf50', mr: 2 }} />
                            <Box>
                                <Typography color="textSecondary" variant="h6">Resueltos</Typography>
                                <Typography variant="h3" fontWeight="bold">{metricas.resueltos}</Typography>
                            </Box>
                        </CardContent>
                    </Card>
                </Grid>
                <Grid item xs={12} sm={4}>
                    <Card elevation={3} sx={{ borderLeft: '5px solid #ff9800' }}>
                        <CardContent sx={{ display: 'flex', alignItems: 'center' }}>
                            <PendingActionsIcon sx={{ fontSize: 50, color: '#ff9800', mr: 2 }} />
                            <Box>
                                <Typography color="textSecondary" variant="h6">Pendientes</Typography>
                                <Typography variant="h3" fontWeight="bold">{metricas.pendientes}</Typography>
                            </Box>
                        </CardContent>
                    </Card>
                </Grid>
            </Grid>

            {/* SECCIÓN DE GRÁFICAS */}
            <Grid container spacing={3}>
                {/* Gráfica de Barras */}
                <Grid item xs={12} md={7}>
                    <Paper elevation={3} sx={{ p: 3, height: 400 }}>
                        <Typography variant="h6" sx={{ mb: 2 }}>Estado Actual de Solicitudes</Typography>
                        <ResponsiveContainer width="100%" height="90%">
                            <BarChart data={datosMensuales} margin={{ top: 5, right: 30, left: 0, bottom: 5 }}>
                                <CartesianGrid strokeDasharray="3 3" />
                                <XAxis dataKey="mes" />
                                <YAxis />
                                <ChartTooltip />
                                <Legend />
                                <Bar dataKey="resueltos" name="Resueltos" fill="#4caf50" radius={[5, 5, 0, 0]} />
                                <Bar dataKey="pendientes" name="Pendientes" fill="#ff9800" radius={[5, 5, 0, 0]} />
                            </BarChart>
                        </ResponsiveContainer>
                    </Paper>
                </Grid>

                {/* Gráfica de Pastel (Carga por Área) */}
                <Grid item xs={12} md={5}>
                    <Paper elevation={3} sx={{ p: 3, height: 400 }}>
                        <Typography variant="h6" sx={{ mb: 2 }}>Carga por Área Dinámica</Typography>
                        <ResponsiveContainer width="100%" height="90%">
                            <PieChart>
                                <Pie
                                    data={metricas.ticketsPorArea}
                                    cx="50%"
                                    cy="50%"
                                    innerRadius={65}
                                    outerRadius={95}
                                    paddingAngle={5}
                                    dataKey="cantidad"
                                    nameKey="nombre"
                                    label
                                >
                                    {metricas.ticketsPorArea.map((entry, index) => (
                                        <Cell key={`cell-${index}`} fill={COLORES[index % COLORES.length]} />
                                    ))}
                                </Pie>
                                <ChartTooltip />
                                <Legend />
                            </PieChart>
                        </ResponsiveContainer>
                    </Paper>
                </Grid>
            </Grid>
        </Box>
    );
}