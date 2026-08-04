import { useState, useEffect } from 'react';
import {
    Box, TextField, Button, Typography, Paper, IconButton, Stack,
    Divider, Autocomplete, Alert
} from '@mui/material';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutlineOutlined';
import AddIcon from '@mui/icons-material/Add';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';

import { equipoService } from '../../services/equipoService';
import { useNotification } from '../../context/NotificationContext.jsx';

/** Fila vacía de equipo. */
const EQUIPO_VACIO = {
    area: '',
    usuarioResponsable: '',
    tipoCpu: '',
    marca: '',
    modelo: '',
    numeroSerie: '',
    memoriaRam: '',
    capacidadDisco: '',
    numeroInventario: '',
};

/**
 * Reporte de mantenimiento preventivo.
 *
 * Cambios respecto a la versión anterior:
 *  - El catálogo de equipos estaba **simulado en el código**: tres entradas
 *    ficticias (DELL INSPIRON, HP PAVILION, ACER POWER) escritas a mano, con
 *    el comentario "esto debería venir de una API real después". El catálogo
 *    real existe en la base y se administra desde su propia pantalla; ahora es
 *    el que alimenta el autocompletado.
 *  - Volcaba el payload completo a consola en cada envío, incluidos los nombres
 *    del personal.
 *  - Los campos autocompletados quedaban `disabled`, así que un equipo que no
 *    estuviera en el catálogo no podía capturarse: había que darlo de alta
 *    primero. Ahora se rellenan al elegir del catálogo pero siguen siendo
 *    editables.
 *  - El nombre del técnico venía de una prop que nunca se pasaba, de modo que
 *    el reporte se firmaba siempre como "SOPORTE TÉCNICO".
 */
export default function MantenimientoPreventivoFormato({
    solicitarPdf,
    generando = false,
    nombreTecnico,
}) {
    const { notificarAdvertencia } = useNotification();

    const [fechaInicio, setFechaInicio] = useState('');
    const [fechaFin, setFechaFin] = useState('');
    const [departamento, setDepartamento] = useState('');
    const [equipos, setEquipos] = useState([]);
    const [catalogo, setCatalogo] = useState([]);

    // --- Catálogo real de equipos -----------------------------------------
    useEffect(() => {
        let cancelado = false;

        // Se pide un tamaño amplio: el catálogo alimenta un autocompletado y
        // paginarlo obligaría a teclear para ver opciones que ya existen.
        equipoService.getAll({ page: 0, size: 200 })
            .then((respuesta) => {
                if (cancelado) return;
                const pagina = respuesta?.data;
                setCatalogo(Array.isArray(pagina?.contenido) ? pagina.contenido : []);
            })
            .catch(() => {
                // Sin catálogo el formulario sigue siendo usable: los campos
                // se capturan a mano.
                if (!cancelado) setCatalogo([]);
            });

        return () => { cancelado = true; };
    }, []);

    const mayusculas = (valor) => (valor ?? '').toUpperCase();

    const agregarEquipo = () => setEquipos((prev) => [...prev, { ...EQUIPO_VACIO }]);

    const eliminarEquipo = (indice) =>
        setEquipos((prev) => prev.filter((_, i) => i !== indice));

    const actualizarEquipo = (indice, campo, valor) => {
        setEquipos((prev) => prev.map((equipo, i) => (
            i === indice ? { ...equipo, [campo]: mayusculas(valor) } : equipo
        )));
    };

    /** Rellena tipo, marca y modelo desde el catálogo, sin bloquear la edición. */
    const aplicarDelCatalogo = (indice, seleccion) => {
        if (!seleccion) return;

        setEquipos((prev) => prev.map((equipo, i) => (
            i === indice
                ? {
                    ...equipo,
                    tipoCpu: mayusculas(seleccion.descripcion),
                    marca: mayusculas(seleccion.marca),
                    modelo: mayusculas(seleccion.modelo),
                }
                : equipo
        )));
    };

    const enviar = async (e) => {
        e.preventDefault();

        if (equipos.length === 0) {
            notificarAdvertencia('Agrega al menos un equipo al reporte.');
            return;
        }

        if (fechaFin && fechaInicio && fechaFin < fechaInicio) {
            notificarAdvertencia('La fecha de fin no puede ser anterior a la de inicio.');
            return;
        }

        await solicitarPdf('mantenimiento', {
            fechaInicio,
            fechaFin,
            nombreTecnico: (nombreTecnico || 'SOPORTE TÉCNICO').toUpperCase(),
            departamento,
            equipos,
        }, 'Mantenimiento preventivo');
    };

    return (
        <Box component="form" onSubmit={enviar} noValidate>
            <Paper variant="outlined" sx={{ p: { xs: 2.5, md: 3.5 } }}>
                <Box
                    sx={{
                        display: 'flex',
                        flexDirection: { xs: 'column', md: 'row' },
                        justifyContent: 'space-between',
                        alignItems: { xs: 'stretch', md: 'center' },
                        gap: 2,
                        mb: 3,
                    }}
                >
                    <Box>
                        <Typography variant="h6" component="h3">
                            Mantenimiento preventivo
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                            Periodo, departamento y equipos atendidos.
                        </Typography>
                    </Box>

                    <Button variant="outlined" onClick={agregarEquipo} startIcon={<AddIcon />}>
                        Agregar equipo
                    </Button>
                </Box>

                <Stack
                    direction={{ xs: 'column', md: 'row' }}
                    spacing={2.5}
                    sx={{ mb: 3 }}
                >
                    <TextField
                        fullWidth
                        type="date"
                        label="Fecha de inicio"
                        slotProps={{ inputLabel: { shrink: true } }}
                        value={fechaInicio}
                        onChange={(e) => setFechaInicio(e.target.value)}
                        required
                    />
                    <TextField
                        fullWidth
                        type="date"
                        label="Fecha de fin"
                        slotProps={{ inputLabel: { shrink: true } }}
                        value={fechaFin}
                        onChange={(e) => setFechaFin(e.target.value)}
                        required
                    />
                    <TextField
                        fullWidth
                        label="Departamento"
                        value={departamento}
                        onChange={(e) => setDepartamento(mayusculas(e.target.value))}
                        required
                    />
                </Stack>

                <Divider sx={{ mb: 3 }} />

                <Typography variant="subtitle2" sx={{ mb: 2, fontWeight: 600 }}>
                    Equipos atendidos ({equipos.length})
                </Typography>

                {equipos.length === 0 && (
                    <Alert severity="info" sx={{ mb: 2 }}>
                        Agrega los equipos a los que se dio mantenimiento. Puedes elegirlos
                        del catálogo para no capturar marca y modelo a mano.
                    </Alert>
                )}

                <Stack spacing={2.5}>
                    {equipos.map((equipo, indice) => (
                        <Paper
                            key={indice}
                            variant="outlined"
                            sx={{ p: 2.5, position: 'relative' }}
                        >
                            <IconButton
                                color="error"
                                onClick={() => eliminarEquipo(indice)}
                                sx={{ position: 'absolute', top: 8, right: 8 }}
                                aria-label={`Quitar el equipo ${indice + 1} del reporte`}
                            >
                                <DeleteOutlineIcon />
                            </IconButton>

                            <Typography variant="overline" color="text.secondary">
                                Equipo {indice + 1}
                            </Typography>

                            <Stack spacing={2.5} sx={{ mt: 1.5 }}>
                                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2.5}>
                                    <TextField
                                        fullWidth
                                        size="small"
                                        label="Área o cubículo"
                                        value={equipo.area}
                                        onChange={(e) => actualizarEquipo(indice, 'area', e.target.value)}
                                        required
                                    />
                                    <TextField
                                        fullWidth
                                        size="small"
                                        label="Usuario responsable"
                                        value={equipo.usuarioResponsable}
                                        onChange={(e) => actualizarEquipo(indice, 'usuarioResponsable', e.target.value)}
                                        required
                                    />
                                </Stack>

                                <Autocomplete
                                    options={catalogo}
                                    getOptionLabel={(o) =>
                                        [o.descripcion, o.marca, o.modelo].filter(Boolean).join(' · ')}
                                    isOptionEqualToValue={(o, v) => o.id === v.id}
                                    onChange={(_e, valor) => aplicarDelCatalogo(indice, valor)}
                                    noOptionsText="El catálogo de equipos está vacío"
                                    renderInput={(params) => (
                                        <TextField
                                            {...params}
                                            size="small"
                                            label="Buscar en el catálogo"
                                            helperText="Rellena tipo, marca y modelo. Puedes corregirlos después."
                                        />
                                    )}
                                />

                                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2.5}>
                                    <TextField
                                        fullWidth
                                        size="small"
                                        label="Tipo de equipo"
                                        value={equipo.tipoCpu}
                                        onChange={(e) => actualizarEquipo(indice, 'tipoCpu', e.target.value)}
                                        required
                                    />
                                    <TextField
                                        fullWidth
                                        size="small"
                                        label="Marca"
                                        value={equipo.marca}
                                        onChange={(e) => actualizarEquipo(indice, 'marca', e.target.value)}
                                    />
                                    <TextField
                                        fullWidth
                                        size="small"
                                        label="Modelo"
                                        value={equipo.modelo}
                                        onChange={(e) => actualizarEquipo(indice, 'modelo', e.target.value)}
                                    />
                                </Stack>

                                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2.5}>
                                    <TextField
                                        fullWidth
                                        size="small"
                                        label="Número de serie"
                                        value={equipo.numeroSerie}
                                        onChange={(e) => actualizarEquipo(indice, 'numeroSerie', e.target.value)}
                                        required
                                    />
                                    <TextField
                                        fullWidth
                                        size="small"
                                        label="Número de inventario"
                                        value={equipo.numeroInventario}
                                        onChange={(e) => actualizarEquipo(indice, 'numeroInventario', e.target.value)}
                                    />
                                </Stack>

                                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2.5}>
                                    <TextField
                                        fullWidth
                                        size="small"
                                        label="Memoria RAM"
                                        value={equipo.memoriaRam}
                                        onChange={(e) => actualizarEquipo(indice, 'memoriaRam', e.target.value)}
                                        required
                                    />
                                    <TextField
                                        fullWidth
                                        size="small"
                                        label="Disco duro"
                                        value={equipo.capacidadDisco}
                                        onChange={(e) => actualizarEquipo(indice, 'capacidadDisco', e.target.value)}
                                        required
                                    />
                                </Stack>
                            </Stack>
                        </Paper>
                    ))}
                </Stack>

                <Box sx={{ mt: 3, display: 'flex', justifyContent: 'flex-end' }}>
                    <Button
                        type="submit"
                        variant="contained"
                        size="large"
                        startIcon={<PictureAsPdfIcon />}
                        disabled={equipos.length === 0 || generando}
                    >
                        {generando ? 'Generando…' : 'Generar reporte'}
                    </Button>
                </Box>
            </Paper>
        </Box>
    );
}
