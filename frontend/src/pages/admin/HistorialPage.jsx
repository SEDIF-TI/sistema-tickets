import { useState } from 'react';
import { Box, Typography, Paper, Tabs, Tab } from '@mui/material';
import HistoryIcon from '@mui/icons-material/History';

import HistorialDictamenes from './HistorialDictamenes.jsx';
import HistorialResguardos from './HistorialResguardos.jsx';
import TicketsPage from '../tickets/TicketsPage.jsx';

/**
 * Historial unificado del administrador.
 *
 * Reúne en pestañas las tres consultas de seguimiento de la institución
 * —dictámenes técnicos, resguardos y tickets— bajo una sola entrada de menú y
 * un solo encabezado, igual que el generador de documentos.
 *
 * La página no consulta nada por sí misma: cada pestaña es la pantalla completa
 * correspondiente, montada con `sinCabecera` para que el título lo ponga esta y
 * no se repita. `/admin/bitacora` y `/admin/resguardos` siguen declaradas en
 * App.jsx para que los enlaces guardados abran las pantallas sueltas.
 */
const PESTANAS = [
    { id: 'dictamenes', etiqueta: 'Dictámenes técnicos' },
    { id: 'resguardos', etiqueta: 'Resguardos' },
    { id: 'tickets', etiqueta: 'Tickets' },
];

export default function HistorialPage() {
    const [pestana, setPestana] = useState(0);

    return (
        <Box>
            <Box sx={{ mb: 3 }}>
                <Typography
                    variant="h4"
                    component="h2"
                    color="primary.main"
                    sx={{ display: 'flex', alignItems: 'center', gap: 1 }}
                >
                    <HistoryIcon fontSize="large" aria-hidden="true" />
                    Historial
                </Typography>
                <Typography variant="body2" color="text.secondary">
                    Dictámenes, resguardos y tickets registrados en la institución.
                </Typography>
            </Box>

            <Paper variant="outlined" sx={{ mb: 3 }}>
                <Tabs
                    value={pestana}
                    onChange={(_e, valor) => setPestana(valor)}
                    variant="scrollable"
                    scrollButtons="auto"
                    allowScrollButtonsMobile
                    aria-label="Secciones del historial"
                >
                    {PESTANAS.map((p) => (
                        <Tab key={p.id} label={p.etiqueta} />
                    ))}
                </Tabs>
            </Paper>

            {/* Se monta solo la pestaña activa, en lugar de ocultar las otras
                con CSS: mantener las tres vivas dispararía sus tres consultas
                paginadas en cada visita a la pantalla. */}
            {pestana === 0 && <HistorialDictamenes />}
            {pestana === 1 && <HistorialResguardos sinCabecera />}
            {pestana === 2 && <TicketsPage sinCabecera />}
        </Box>
    );
}
