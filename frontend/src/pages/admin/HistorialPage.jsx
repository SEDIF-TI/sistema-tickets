import { useState } from 'react';
import { Box, Typography, Paper, Tabs, Tab } from '@mui/material';
import HistoryIcon from '@mui/icons-material/History';

import HistorialDictamenes from './HistorialDictamenes.jsx';
import HistorialResguardos from './HistorialResguardos.jsx';
import TicketsPage from '../tickets/TicketsPage.jsx';

/**
 * Historial unificado del área.
 *
 * Antes eran dos entradas de menú separadas —«Bitácora» e «Historial de
 * resguardos»— y los dictámenes no tenían ninguna, porque ni siquiera se
 * guardaban. Aquí conviven las tres consultas en pestañas, igual que en el
 * generador de documentos.
 *
 * Las rutas antiguas (`/admin/bitacora` y `/admin/resguardos`) siguen
 * declaradas en App.jsx a propósito: un enlace guardado debe seguir abriendo
 * algo. Lo que se retiró en la migración V10 es su entrada de menú, no su
 * acceso.
 *
 * Cada pestaña monta la pantalla con `sinCabecera`, para que el título salga
 * una sola vez.
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

            {/* Se monta solo la pestaña activa: mantener las tres vivas
                dispararía tres consultas paginadas en cada visita. */}
            {pestana === 0 && <HistorialDictamenes />}
            {pestana === 1 && <HistorialResguardos sinCabecera />}
            {pestana === 2 && <TicketsPage sinCabecera />}
        </Box>
    );
}
