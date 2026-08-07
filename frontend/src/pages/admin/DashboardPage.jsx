import { useState, useEffect, useCallback } from 'react';
import {
    Box, Typography, CircularProgress, Alert, Button, Stack, useMediaQuery
} from '@mui/material';
import { useTheme } from '@mui/material/styles';
import {
    BarChart, Bar, LineChart, Line, PieChart, Pie, Cell,
    XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, LabelList
} from 'recharts';

import DashboardIcon from '@mui/icons-material/Dashboard';
import ConfirmationNumberIcon from '@mui/icons-material/ConfirmationNumber';
import GroupIcon from '@mui/icons-material/Group';
import ListAltIcon from '@mui/icons-material/ListAlt';
import LocalOfferIcon from '@mui/icons-material/LocalOffer';
import CalendarMonthIcon from '@mui/icons-material/CalendarMonth';
import EngineeringIcon from '@mui/icons-material/Engineering';
import StarBorderIcon from '@mui/icons-material/StarBorder';
import DomainIcon from '@mui/icons-material/Domain';
import CampaignIcon from '@mui/icons-material/Campaign';
import RefreshIcon from '@mui/icons-material/Refresh';
import PrintIcon from '@mui/icons-material/Print';

import { dashboardService } from '../../services/dashboardService';
import { useNotification } from '../../context/NotificationContext.jsx';
import { colorDeEstado, colorDePrioridad, colorDeSerie } from '../../util/paletaGraficas';
import { colorCalificacion, colorPromedio } from '../../util/calificacion';

import TarjetaGrafica from '../../components/TarjetaGrafica';
import TarjetaMetrica from '../../components/TarjetaMetrica';

/** Rejilla recesiva: una línea sólida a un tono de la superficie, sin guiones. */
const REJILLA = { stroke: '#e6e6e3', strokeDasharray: '0' };
const EJE = { fontSize: 11, fill: '#52514e' };

/**
 * Panel de control del administrador: resumen de la operación de soporte.
 *
 * Todo el contenido sale de una sola llamada a `dashboardService.getMetricas()`
 * que devuelve las series ya agregadas por el backend. Mientras responde se
 * muestra un indicador de carga, y si falla, un aviso con botón de reintento en
 * lugar de gráficas vacías.
 *
 * La parte superior son cuatro cifras —tickets registrados, tickets abiertos,
 * usuarios y encuestas respondidas— porque un total no gana nada dibujado como
 * una barra. Debajo, cada `TarjetaGrafica` recibe su serie en `datos` y solo
 * dibuja a su hijo cuando hay algo que mostrar; si la serie viene vacía pinta el
 * texto de `vacio`, lo que además evita que `ResponsiveContainer` mida un padre
 * colapsado. Con `columnas` la tarjeta ofrece también una vista de tabla, para
 * leer los valores sin depender del color.
 *
 * Los colores de estado, prioridad y calificación se piden por categoría a las
 * funciones de `paletaGraficas` y `calificacion`, nunca por posición en la
 * lista: así una misma categoría conserva su color aunque cambie de sitio.
 *
 * El botón de imprimir usa `window.print()` sobre la propia página; la cabecera
 * lleva `ocultar-al-imprimir` para no salir en el papel.
 */
export default function DashboardPage() {
    const { notificarError } = useNotification();
    const theme = useTheme();
    const esMovil = useMediaQuery(theme.breakpoints.down('md'));

    const [datos, setDatos] = useState(null);
    const [error, setError] = useState('');
    const [cargando, setCargando] = useState(true);

    const cargar = useCallback(async () => {
        try {
            const respuesta = await dashboardService.getMetricas();
            setDatos(respuesta?.data ?? null);
            setError('');
        } catch (err) {
            setError(err?.mensaje || 'No se pudieron cargar las métricas del panel.');
            notificarError(err);
        } finally {
            setCargando(false);
        }
    }, [notificarError]);

    useEffect(() => { cargar(); }, [cargar]);

    if (cargando) {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 10 }}>
                <CircularProgress aria-label="Cargando las métricas" />
            </Box>
        );
    }

    if (error) {
        return (
            <Alert
                severity="error"
                action={
                    <Button color="inherit" size="small" onClick={cargar}>
                        Reintentar
                    </Button>
                }
            >
                {error}
            </Alert>
        );
    }

    const porEstatus = datos?.porEstatus ?? [];
    const porPrioridad = datos?.porPrioridad ?? [];
    const porArea = datos?.porArea ?? [];
    const porIngeniero = datos?.porIngeniero ?? [];
    const porFecha = datos?.porFecha ?? [];
    const avisosPorArea = datos?.avisosPorArea ?? [];
    const porCalificacion = datos?.porCalificacion ?? [];
    const calificacionPorTecnico = datos?.calificacionPorTecnico ?? [];

    // El backend envía las encuestas repartidas por calificación, no el total.
    const totalEncuestas = porCalificacion.reduce((suma, c) => suma + (c.cantidad ?? 0), 0);

    const abiertos = porEstatus.find((e) => e.nombre === 'Abierto')?.cantidad ?? 0;

    return (
        <Box>
            <Box
                sx={{
                    display: 'flex',
                    flexDirection: { xs: 'column', sm: 'row' },
                    justifyContent: 'space-between',
                    alignItems: { xs: 'stretch', sm: 'center' },
                    gap: 2,
                    mb: 3,
                }}
                className="ocultar-al-imprimir"
            >
                <Box>
                    <Typography
                        variant="h4"
                        component="h2"
                        color="primary.main"
                        sx={{ display: 'flex', alignItems: 'center', gap: 1 }}
                    >
                        <DashboardIcon fontSize="large" aria-hidden="true" />
                        Panel de control
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        Resumen de la operación del área de soporte.
                    </Typography>
                </Box>

                <Stack direction="row" spacing={1}>
                    <Button startIcon={<RefreshIcon />} onClick={cargar}>
                        Actualizar
                    </Button>
                    <Button
                        variant="outlined"
                        startIcon={<PrintIcon />}
                        onClick={() => window.print()}
                    >
                        Imprimir
                    </Button>
                </Stack>
            </Box>

            <Box
                sx={{
                    display: 'grid',
                    gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', lg: 'repeat(4, 1fr)' },
                    gap: 2,
                    mb: 3,
                }}
            >
                <TarjetaMetrica
                    etiqueta="Tickets registrados"
                    valor={datos?.totalTickets ?? 0}
                    icono={ConfirmationNumberIcon}
                    detalle="Histórico completo"
                />
                <TarjetaMetrica
                    etiqueta="Tickets abiertos"
                    valor={abiertos}
                    icono={ListAltIcon}
                    detalle="Pendientes de atender"
                    color={abiertos > 0 ? 'warning.main' : 'success.main'}
                />
                <TarjetaMetrica
                    etiqueta="Usuarios del sistema"
                    valor={datos?.totalUsuarios ?? 0}
                    icono={GroupIcon}
                    detalle="Altas registradas"
                />
                <TarjetaMetrica
                    etiqueta="Encuestas respondidas"
                    valor={totalEncuestas}
                    icono={StarBorderIcon}
                    detalle="Sobre el servicio recibido"
                />
            </Box>

            <Box
                sx={{
                    display: 'grid',
                    gridTemplateColumns: { xs: '1fr', lg: 'repeat(2, 1fr)' },
                    gap: 2,
                }}
            >
                <TarjetaGrafica
                    titulo="Tickets por estado"
                    icono={ListAltIcon}
                    datos={porEstatus}
                    alto={300}
                    vacio="Cuando se levanten tickets, aquí verás cómo se reparten por estado."
                >
                    <ResponsiveContainer>
                        {/* Parte de un todo, con pocas porciones: es el caso en
                            el que un anillo se lee de un vistazo. */}
                        <PieChart>
                            <Pie
                                data={porEstatus}
                                dataKey="cantidad"
                                nameKey="nombre"
                                innerRadius={62}
                                outerRadius={95}
                                paddingAngle={2}
                                // 2px de superficie entre porciones en lugar de
                                // un borde dibujado alrededor de cada una.
                                stroke={theme.palette.background.paper}
                                strokeWidth={2}
                            >
                                {porEstatus.map((entrada) => (
                                    <Cell key={entrada.nombre} fill={colorDeEstado(entrada.nombre)} />
                                ))}
                            </Pie>
                            <Tooltip />
                            <Legend verticalAlign="bottom" height={32} />
                        </PieChart>
                    </ResponsiveContainer>
                </TarjetaGrafica>

                <TarjetaGrafica
                    titulo="Tickets por prioridad"
                    icono={LocalOfferIcon}
                    datos={porPrioridad}
                    alto={300}
                    vacio="La prioridad se elige al levantar el ticket."
                >
                    <ResponsiveContainer>
                        {/* Barras y no pastel: comparar magnitudes cercanas
                            entre porciones es justo lo que peor lee el ojo. */}
                        <BarChart data={porPrioridad} margin={{ top: 16, right: 16, left: 0, bottom: 8 }}>
                            <CartesianGrid {...REJILLA} vertical={false} />
                            <XAxis dataKey="nombre" tick={EJE} tickLine={false} />
                            <YAxis tick={EJE} tickLine={false} axisLine={false} allowDecimals={false} />
                            <Tooltip cursor={{ fill: 'rgba(0,0,0,0.04)' }} />
                            <Bar dataKey="cantidad" name="Tickets" barSize={38} radius={[4, 4, 0, 0]}>
                                {porPrioridad.map((entrada) => (
                                    <Cell key={entrada.nombre} fill={colorDePrioridad(entrada.nombre)} />
                                ))}
                            </Bar>
                        </BarChart>
                    </ResponsiveContainer>
                </TarjetaGrafica>

                <TarjetaGrafica
                    titulo="Calificación del servicio"
                    icono={StarBorderIcon}
                    datos={porCalificacion}
                    alto={300}
                    vacio="Cuando los solicitantes respondan la encuesta, verás aquí cómo valoran el servicio."
                >
                    <ResponsiveContainer>
                        <PieChart>
                            <Pie
                                data={porCalificacion}
                                dataKey="cantidad"
                                nameKey="nombre"
                                innerRadius={62}
                                outerRadius={95}
                                paddingAngle={2}
                                stroke={theme.palette.background.paper}
                                strokeWidth={2}
                            >
                                {porCalificacion.map((entrada) => (
                                    <Cell
                                        key={entrada.nombre}
                                        fill={theme.palette[colorCalificacion(entrada.nombre)]?.main ?? '#8b8a85'}
                                    />
                                ))}
                            </Pie>
                            <Tooltip />
                            <Legend verticalAlign="bottom" height={32} />
                        </PieChart>
                    </ResponsiveContainer>
                </TarjetaGrafica>

                <TarjetaGrafica
                    titulo="Calificación por técnico"
                    icono={EngineeringIcon}
                    datos={calificacionPorTecnico}
                    alto={Math.max(280, calificacionPorTecnico.length * 34 + 60)}
                    columnas={[
                        { id: 'nombre', etiqueta: 'Técnico' },
                        { id: 'promedio', etiqueta: 'Promedio' },
                        { id: 'respuestas', etiqueta: 'Respuestas' },
                    ]}
                    vacio="Aquí verás el promedio de cada técnico cuando haya encuestas respondidas."
                >
                    <ResponsiveContainer>
                        <BarChart
                            data={calificacionPorTecnico}
                            layout="vertical"
                            margin={{ top: 8, right: 60, left: 8, bottom: 8 }}
                        >
                            <CartesianGrid {...REJILLA} horizontal={false} />
                            {/* La escala va de 1 a 3 y se fija: dejarla
                                automática exageraría diferencias mínimas al
                                estirar las barras a todo el ancho. */}
                            <XAxis
                                type="number"
                                domain={[0, 3]}
                                ticks={[0, 1, 2, 3]}
                                tick={EJE}
                                tickLine={false}
                                axisLine={false}
                            />
                            <YAxis
                                type="category"
                                dataKey="nombre"
                                width={esMovil ? 90 : 150}
                                tick={EJE}
                                tickLine={false}
                                axisLine={false}
                            />
                            <Tooltip
                                cursor={{ fill: 'rgba(0,0,0,0.04)' }}
                                formatter={(valor, _n, item) => [
                                    `${valor} de 3 · ${item?.payload?.respuestas ?? 0} respuesta(s)`,
                                    'Promedio',
                                ]}
                            />
                            <Bar dataKey="promedio" name="Promedio" barSize={22} radius={[0, 4, 4, 0]}>
                                {calificacionPorTecnico.map((entrada) => (
                                    <Cell key={entrada.nombre} fill={colorPromedio(entrada.promedio)} />
                                ))}
                                {/* El tooltip acompaña el promedio con el número
                                    de respuestas: una media de 3.0 sobre una
                                    sola encuesta no dice lo mismo que sobre
                                    cuarenta. */}
                                <LabelList
                                    dataKey="promedio"
                                    position="right"
                                    formatter={(v) => `${v}`}
                                    style={{ fontSize: 11, fontWeight: 600, fill: '#52514e' }}
                                />
                            </Bar>
                        </BarChart>
                    </ResponsiveContainer>
                </TarjetaGrafica>

                <Box sx={{ gridColumn: { lg: 'span 2' } }}>
                    <TarjetaGrafica
                        titulo="Tickets por día"
                        icono={CalendarMonthIcon}
                        datos={porFecha}
                        alto={300}
                        columnas={[
                            { id: 'fecha', etiqueta: 'Fecha' },
                            { id: 'total', etiqueta: 'Tickets' },
                        ]}
                        vacio="Aquí verás la carga diaria conforme se levanten tickets."
                    >
                        <ResponsiveContainer>
                            <LineChart data={porFecha} margin={{ top: 16, right: 24, left: 0, bottom: 8 }}>
                                <CartesianGrid {...REJILLA} vertical={false} />
                                <XAxis dataKey="fecha" tick={EJE} tickLine={false} />
                                <YAxis tick={EJE} tickLine={false} axisLine={false} allowDecimals={false} />
                                <Tooltip />
                                <Line
                                    type="monotone"
                                    dataKey="total"
                                    name="Tickets"
                                    stroke={colorDeSerie(0)}
                                    strokeWidth={2}
                                    dot={{ r: 3 }}
                                    activeDot={{ r: 6 }}
                                >
                                    {/* Solo el último punto lleva número: una
                                        etiqueta por punto es ilegible. */}
                                    <LabelList
                                        dataKey="total"
                                        position="top"
                                        offset={10}
                                        style={{ fontSize: 11, fontWeight: 600, fill: '#52514e' }}
                                        content={({ x, y, value, index }) =>
                                            index === porFecha.length - 1 ? (
                                                <text x={x} y={y - 8} textAnchor="middle"
                                                    style={{ fontSize: 11, fontWeight: 600, fill: '#52514e' }}>
                                                    {value}
                                                </text>
                                            ) : null
                                        }
                                    />
                                </Line>
                            </LineChart>
                        </ResponsiveContainer>
                    </TarjetaGrafica>
                </Box>

                <TarjetaGrafica
                    titulo="Tickets por área"
                    icono={DomainIcon}
                    datos={porArea}
                    alto={Math.max(280, porArea.length * 34 + 60)}
                    vacio="El área sale del usuario que levanta cada ticket."
                >
                    <ResponsiveContainer>
                        {/* Barras horizontales: los nombres de área son largos
                            y en vertical habría que girarlos. */}
                        <BarChart
                            data={porArea}
                            layout="vertical"
                            margin={{ top: 8, right: 40, left: 8, bottom: 8 }}
                        >
                            <CartesianGrid {...REJILLA} horizontal={false} />
                            <XAxis type="number" tick={EJE} tickLine={false} axisLine={false} allowDecimals={false} />
                            <YAxis
                                type="category"
                                dataKey="nombre"
                                width={esMovil ? 90 : 150}
                                tick={EJE}
                                tickLine={false}
                                axisLine={false}
                            />
                            <Tooltip cursor={{ fill: 'rgba(0,0,0,0.04)' }} />
                            <Bar
                                dataKey="cantidad"
                                name="Tickets"
                                fill={colorDeSerie(0)}
                                barSize={22}
                                radius={[0, 4, 4, 0]}
                            >
                                <LabelList
                                    dataKey="cantidad"
                                    position="right"
                                    style={{ fontSize: 11, fontWeight: 600, fill: '#52514e' }}
                                />
                            </Bar>
                        </BarChart>
                    </ResponsiveContainer>
                </TarjetaGrafica>

                <TarjetaGrafica
                    titulo="Tickets atendidos por técnico"
                    icono={EngineeringIcon}
                    datos={porIngeniero}
                    alto={Math.max(280, porIngeniero.length * 34 + 60)}
                    vacio="Aquí verás la carga de cada técnico cuando se asignen tickets."
                >
                    <ResponsiveContainer>
                        <BarChart
                            data={porIngeniero}
                            layout="vertical"
                            margin={{ top: 8, right: 40, left: 8, bottom: 8 }}
                        >
                            <CartesianGrid {...REJILLA} horizontal={false} />
                            <XAxis type="number" tick={EJE} tickLine={false} axisLine={false} allowDecimals={false} />
                            <YAxis
                                type="category"
                                dataKey="nombre"
                                width={esMovil ? 90 : 150}
                                tick={EJE}
                                tickLine={false}
                                axisLine={false}
                            />
                            <Tooltip cursor={{ fill: 'rgba(0,0,0,0.04)' }} />
                            <Bar
                                dataKey="cantidad"
                                name="Tickets"
                                fill={colorDeSerie(2)}
                                barSize={22}
                                radius={[0, 4, 4, 0]}
                            >
                                <LabelList
                                    dataKey="cantidad"
                                    position="right"
                                    style={{ fontSize: 11, fontWeight: 600, fill: '#52514e' }}
                                />
                            </Bar>
                        </BarChart>
                    </ResponsiveContainer>
                </TarjetaGrafica>

                {avisosPorArea.length > 0 && (
                    <Box sx={{ gridColumn: { lg: 'span 2' } }}>
                        <TarjetaGrafica
                            titulo="Avisos activos por área"
                            icono={CampaignIcon}
                            datos={avisosPorArea}
                            alto={Math.max(240, avisosPorArea.length * 34 + 60)}
                            columnas={[
                                { id: 'nombre', etiqueta: 'Área' },
                                { id: 'cantidad', etiqueta: 'Avisos' },
                            ]}
                        >
                            <ResponsiveContainer>
                                <BarChart
                                    data={avisosPorArea}
                                    layout="vertical"
                                    margin={{ top: 8, right: 40, left: 8, bottom: 8 }}
                                >
                                    <CartesianGrid {...REJILLA} horizontal={false} />
                                    <XAxis type="number" tick={EJE} tickLine={false} axisLine={false} allowDecimals={false} />
                                    <YAxis
                                        type="category"
                                        dataKey="nombre"
                                        width={esMovil ? 90 : 150}
                                        tick={EJE}
                                        tickLine={false}
                                        axisLine={false}
                                    />
                                    <Tooltip cursor={{ fill: 'rgba(0,0,0,0.04)' }} />
                                    <Bar
                                        dataKey="cantidad"
                                        name="Avisos"
                                        fill={colorDeSerie(3)}
                                        barSize={22}
                                        radius={[0, 4, 4, 0]}
                                    >
                                        <LabelList
                                            dataKey="cantidad"
                                            position="right"
                                            style={{ fontSize: 11, fontWeight: 600, fill: '#52514e' }}
                                        />
                                    </Bar>
                                </BarChart>
                            </ResponsiveContainer>
                        </TarjetaGrafica>
                    </Box>
                )}
            </Box>
        </Box>
    );
}
