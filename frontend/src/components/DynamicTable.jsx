import { useState, useMemo } from 'react';
import {
    Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
    TableSortLabel, TablePagination, Typography, Box, Toolbar, TextField,
    InputAdornment, IconButton, Tooltip, Chip, Stack, Menu, MenuItem,
    ListItemIcon, ListItemText, Divider, useMediaQuery, Card, CardContent
} from '@mui/material';
import { useTheme } from '@mui/material/styles';
import SearchIcon from '@mui/icons-material/Search';
import ClearIcon from '@mui/icons-material/Clear';
import FilterListIcon from '@mui/icons-material/FilterList';
import RefreshIcon from '@mui/icons-material/Refresh';
import ViewColumnIcon from '@mui/icons-material/ViewColumn';
import CheckIcon from '@mui/icons-material/Check';

import TableSkeleton from './TableSkeleton';
import EmptyState from './EmptyState';

/**
 * Tabla del sistema.
 *
 * Resuelve de forma uniforme lo que cada pantalla venía reimplementando:
 * búsqueda, filtros, ordenación, paginación, estado de carga, estado vacío y
 * comportamiento en móvil.
 *
 * **La paginación y la ordenación las resuelve el backend.** Este componente
 * solo muestra los controles y avisa del cambio; nunca trocea ni reordena en
 * memoria, porque solo tiene la página actual, no el conjunto completo.
 *
 * En pantallas estrechas la tabla se convierte en tarjetas: una fila de ocho
 * columnas es ilegible en un teléfono, incluso con scroll horizontal.
 *
 * Definición de columnas:
 *
 *   const columnas = [
 *     { id: 'id',      etiqueta: 'ID',     ancho: '80px', ordenable: true },
 *     { id: 'titulo',  etiqueta: 'Título', principal: true },
 *     { id: 'estatus', etiqueta: 'Estado', alineacion: 'center',
 *       render: (fila) => <Chip label={fila.estatus} /> },
 *     { id: 'acciones', etiqueta: 'Acciones', alineacion: 'center',
 *       sinOrden: true, render: (fila) => <BotonesAccion fila={fila} /> },
 *   ];
 *
 * `principal: true` marca la columna que hace de título en la vista de
 * tarjetas. Si no se indica, se usa la primera.
 */
export default function DynamicTable({
    columnas = [],
    filas = [],
    cargando = false,
    claveFila = 'id',
    anchoMinimo = 800,
    vacio = {},
    onClickFila,
    // Paginación de servidor
    paginacion = null,
    onCambiarPagina,
    onCambiarTamano,
    // Ordenación de servidor
    orden = null,
    onCambiarOrden,
    // Búsqueda y filtros
    busqueda = '',
    onBuscar,
    placeholderBusqueda = 'Buscar…',
    filtros = [],
    onRecargar,
    titulo,
    acciones,
}) {
    const theme = useTheme();
    const esMovil = useMediaQuery(theme.breakpoints.down('md'));

    // Columnas ocultas por el usuario (solo en escritorio).
    const [ocultas, setOcultas] = useState([]);
    const [anclaColumnas, setAnclaColumnas] = useState(null);

    // Se descartan primero las columnas que la pantalla marca como `oculta`
    // —normalmente por rol— y luego las que el usuario apago desde el menu.
    // Sin lo primero, una pantalla no podia esconder una columna que no
    // corresponde a quien la mira.
    const columnasAplicables = useMemo(
        () => columnas.filter((c) => !c.oculta),
        [columnas]
    );

    const columnasVisibles = useMemo(
        () => columnasAplicables.filter((c) => !ocultas.includes(c.id)),
        [columnasAplicables, ocultas]
    );

    const sinDatos = !cargando && filas.length === 0;
    const hayPaginacion = Boolean(paginacion) && !sinDatos;
    const hayBarra = Boolean(onBuscar || filtros.length > 0 || onRecargar || titulo || acciones);

    // Filtros con valor distinto del predeterminado, para el contador.
    const filtrosActivos = filtros.filter((f) => f.valor && f.valor !== f.valorPorDefecto).length;

    const alternarColumna = (id) => {
        setOcultas((prev) => (prev.includes(id) ? prev.filter((c) => c !== id) : [...prev, id]));
    };

    const alOrdenar = (campo) => {
        if (!onCambiarOrden) return;
        const esMismo = orden?.campo === campo;
        const direccion = esMismo && orden?.direccion === 'asc' ? 'desc' : 'asc';
        onCambiarOrden(campo, direccion);
    };

    // ---------------------------------------------------------------- barra
    const barraHerramientas = hayBarra && (
        <Toolbar
            className="barra-filtros"
            disableGutters
            sx={{
                px: 2,
                py: 1.5,
                flexWrap: 'wrap',
                gap: 1.5,
                borderBottom: '1px solid',
                borderColor: 'divider',
                bgcolor: 'grey.50',
            }}
        >
            {titulo && (
                <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                    {titulo}
                </Typography>
            )}

            {onBuscar && (
                <TextField
                    size="small"
                    placeholder={placeholderBusqueda}
                    value={busqueda}
                    onChange={(e) => onBuscar(e.target.value)}
                    sx={{
                        // Ancho generoso pero acotado: un buscador a todo lo
                        // ancho desequilibra la barra en pantallas grandes.
                        flexGrow: 1,
                        minWidth: { xs: '100%', sm: 240 },
                        maxWidth: { sm: 360 },
                        bgcolor: 'background.paper',
                    }}
                    InputProps={{
                        startAdornment: (
                            <InputAdornment position="start">
                                <SearchIcon fontSize="small" color="action" />
                            </InputAdornment>
                        ),
                        endAdornment: busqueda ? (
                            <InputAdornment position="end">
                                <IconButton
                                    size="small"
                                    onClick={() => onBuscar('')}
                                    aria-label="Limpiar búsqueda"
                                >
                                    <ClearIcon fontSize="small" />
                                </IconButton>
                            </InputAdornment>
                        ) : null,
                    }}
                />
            )}

            {/* Cada filtro decide su propio ancho según lo que muestra: un
                estado necesita menos espacio que un nombre de área. */}
            {filtros.map((filtro) => (
                <TextField
                    key={filtro.id}
                    select
                    size="small"
                    label={filtro.etiqueta}
                    value={filtro.valor ?? ''}
                    onChange={(e) => filtro.onChange(e.target.value)}
                    sx={{
                        minWidth: filtro.ancho ?? 170,
                        bgcolor: 'background.paper',
                        flexShrink: 0,
                    }}
                >
                    {filtro.opciones.map((op) => (
                        <MenuItem key={op.valor} value={op.valor}>
                            {op.etiqueta}
                        </MenuItem>
                    ))}
                </TextField>
            ))}

            {filtrosActivos > 0 && (
                <Chip
                    size="small"
                    icon={<FilterListIcon />}
                    label={`${filtrosActivos} filtro${filtrosActivos > 1 ? 's' : ''}`}
                    onDelete={() => filtros.forEach((f) => f.onChange(f.valorPorDefecto ?? ''))}
                    color="primary"
                    variant="outlined"
                />
            )}

            <Box sx={{ flexGrow: { xs: 0, sm: 1 } }} />

            {acciones}

            {onRecargar && (
                <Tooltip title="Recargar">
                    <IconButton onClick={onRecargar} size="small" aria-label="Recargar la tabla">
                        <RefreshIcon />
                    </IconButton>
                </Tooltip>
            )}

            {/* Selector de columnas: útil en tablas anchas, innecesario en
                móvil, donde ya se muestran como tarjetas. */}
            {!esMovil && columnasAplicables.length > 4 && (
                <>
                    <Tooltip title="Mostrar u ocultar columnas">
                        <IconButton
                            onClick={(e) => setAnclaColumnas(e.currentTarget)}
                            size="small"
                            aria-label="Configurar columnas visibles"
                        >
                            <ViewColumnIcon />
                        </IconButton>
                    </Tooltip>
                    <Menu
                        anchorEl={anclaColumnas}
                        open={Boolean(anclaColumnas)}
                        onClose={() => setAnclaColumnas(null)}
                    >
                        <MenuItem disabled sx={{ opacity: '1 !important' }}>
                            <Typography variant="caption" color="text.secondary">
                                Columnas visibles
                            </Typography>
                        </MenuItem>
                        <Divider />
                        {columnasAplicables.map((col) => (
                            <MenuItem key={col.id} onClick={() => alternarColumna(col.id)} dense>
                                <ListItemIcon sx={{ minWidth: 32 }}>
                                    {!ocultas.includes(col.id) && <CheckIcon fontSize="small" />}
                                </ListItemIcon>
                                <ListItemText primary={col.etiqueta} />
                            </MenuItem>
                        ))}
                    </Menu>
                </>
            )}
        </Toolbar>
    );

    const controlesPaginacion = (opciones) =>
        hayPaginacion && (
            <TablePagination
                component="div"
                count={paginacion.totalItems ?? 0}
                page={paginacion.pagina ?? 0}
                rowsPerPage={paginacion.tamano ?? 10}
                rowsPerPageOptions={opciones}
                onPageChange={(_e, p) => onCambiarPagina?.(p)}
                onRowsPerPageChange={(e) => {
                    // Al cambiar el tamaño se vuelve a la primera página: la
                    // página 7 de un listado de 10 puede no existir con 50.
                    onCambiarTamano?.(parseInt(e.target.value, 10));
                }}
                labelRowsPerPage="Filas por página"
                labelDisplayedRows={({ from, to, count }) =>
                    `${from}–${to} de ${count !== -1 ? count : `más de ${to}`}`
                }
                getItemAriaLabel={(t) => (t === 'previous' ? 'Página anterior' : 'Página siguiente')}
                sx={{ borderTop: '1px solid', borderColor: 'divider' }}
            />
        );

    // ------------------------------------------------------- vista tarjetas
    if (esMovil && !cargando && !sinDatos) {
        const columnaPrincipal = columnasAplicables.find((c) => c.principal) ?? columnasAplicables[0];
        const columnaAcciones = columnasAplicables.find((c) => c.id === 'acciones');
        const columnasDetalle = columnasAplicables.filter(
            (c) => c.id !== columnaPrincipal.id && c.id !== 'acciones'
        );

        return (
            <Paper variant="outlined" sx={{ overflow: 'hidden' }}>
                {barraHerramientas}

                <Stack spacing={0}>
                    {filas.map((fila, indice) => (
                        <Card
                            key={fila[claveFila] ?? indice}
                            elevation={0}
                            onClick={onClickFila ? () => onClickFila(fila) : undefined}
                            sx={{
                                borderRadius: 0,
                                borderBottom: '1px solid',
                                borderColor: 'divider',
                                cursor: onClickFila ? 'pointer' : 'default',
                            }}
                        >
                            <CardContent sx={{ py: 2, '&:last-child': { pb: 2 } }}>
                                <Typography variant="subtitle2" sx={{ fontWeight: 600, mb: 1.5 }}>
                                    {columnaPrincipal.render
                                        ? columnaPrincipal.render(fila)
                                        : fila[columnaPrincipal.id]}
                                </Typography>

                                <Stack spacing={1}>
                                    {columnasDetalle.map((col) => (
                                        <Box
                                            key={col.id}
                                            sx={{
                                                display: 'flex',
                                                justifyContent: 'space-between',
                                                alignItems: 'center',
                                                gap: 2,
                                            }}
                                        >
                                            <Typography variant="caption" color="text.secondary">
                                                {col.etiqueta}
                                            </Typography>
                                            <Box sx={{ textAlign: 'right', minWidth: 0 }}>
                                                {col.render ? (
                                                    col.render(fila)
                                                ) : (
                                                    <Typography variant="body2" noWrap>
                                                        {fila[col.id] ?? '—'}
                                                    </Typography>
                                                )}
                                            </Box>
                                        </Box>
                                    ))}
                                </Stack>

                                {columnaAcciones && (
                                    <Box
                                        sx={{
                                            mt: 2,
                                            pt: 1.5,
                                            borderTop: '1px solid',
                                            borderColor: 'divider',
                                            display: 'flex',
                                            justifyContent: 'flex-end',
                                        }}
                                    >
                                        {columnaAcciones.render(fila)}
                                    </Box>
                                )}
                            </CardContent>
                        </Card>
                    ))}
                </Stack>

                {/* En móvil se oculta el selector de filas por página: ocupa
                    más de lo que aporta en una pantalla estrecha. */}
                {controlesPaginacion([])}
            </Paper>
        );
    }

    // ---------------------------------------------------------- vista tabla
    return (
        <Paper variant="outlined" sx={{ overflow: 'hidden' }}>
            {barraHerramientas}

            {/* El scroll horizontal vive DENTRO del contenedor: la página nunca
                se desplaza en horizontal, solo la tabla. */}
            <TableContainer sx={{ maxWidth: '100%' }}>
                <Table sx={{ minWidth: sinDatos ? 'auto' : anchoMinimo }} aria-busy={cargando}>
                    <TableHead>
                        {/* El `&:hover` se neutraliza aqui y no solo en el
                            tema: este `sx` genera su propia clase, que gana en
                            especificidad al selector global. Sin esto, al pasar
                            el cursor el gris del hover se mezclaba con el
                            guinda y el texto blanco del encabezado quedaba casi
                            ilegible.

                            Se apaga tambien el fondo que MUI pone al control de
                            orden: el encabezado debe quedarse quieto al pasar
                            el cursor, sin ningun cambio de color. */}
                        <TableRow
                            sx={{
                                bgcolor: 'primary.main',
                                '&:hover': { bgcolor: 'primary.main' },
                                '& .MuiTableCell-head': {
                                    '&:hover': { bgcolor: 'transparent' },
                                },
                                '& .MuiTableSortLabel-root:hover': {
                                    bgcolor: 'transparent',
                                    color: 'inherit',
                                },
                            }}
                        >
                            {columnasVisibles.map((col) => {
                                const ordenable = col.ordenable && onCambiarOrden && !col.sinOrden;
                                const activo = orden?.campo === col.id;

                                return (
                                    <TableCell
                                        key={col.id}
                                        align={col.alineacion || 'left'}
                                        sx={{
                                            color: 'primary.contrastText',
                                            width: col.ancho,
                                            borderBottom: 'none',
                                        }}
                                        sortDirection={activo ? orden.direccion : false}
                                    >
                                        {ordenable ? (
                                            <TableSortLabel
                                                active={activo}
                                                direction={activo ? orden.direccion : 'asc'}
                                                onClick={() => alOrdenar(col.id)}
                                            >
                                                {col.etiqueta}
                                            </TableSortLabel>
                                        ) : (
                                            col.etiqueta
                                        )}
                                    </TableCell>
                                );
                            })}
                        </TableRow>
                    </TableHead>

                    {cargando ? (
                        <TableSkeleton columnas={columnasVisibles.length} filas={5} />
                    ) : (
                        <TableBody>
                            {sinDatos ? (
                                <TableRow sx={{ '&:hover': { bgcolor: 'transparent' } }}>
                                    <TableCell
                                        colSpan={columnasVisibles.length}
                                        sx={{ borderBottom: 'none', p: 0 }}
                                    >
                                        <EmptyState {...vacio} />
                                    </TableCell>
                                </TableRow>
                            ) : (
                                filas.map((fila, indice) => (
                                    <TableRow
                                        key={fila[claveFila] ?? indice}
                                        hover
                                        onClick={onClickFila ? () => onClickFila(fila) : undefined}
                                        sx={onClickFila ? { cursor: 'pointer' } : undefined}
                                    >
                                        {columnasVisibles.map((col) => (
                                            <TableCell
                                                key={col.id}
                                                align={col.alineacion || 'left'}
                                                sx={col.sx}
                                            >
                                                {col.render ? (
                                                    col.render(fila)
                                                ) : (
                                                    <Typography variant="body2" component="span">
                                                        {fila[col.id] ?? '—'}
                                                    </Typography>
                                                )}
                                            </TableCell>
                                        ))}
                                    </TableRow>
                                ))
                            )}
                        </TableBody>
                    )}
                </Table>
            </TableContainer>

            {controlesPaginacion([10, 25, 50, 100])}
        </Paper>
    );
}
