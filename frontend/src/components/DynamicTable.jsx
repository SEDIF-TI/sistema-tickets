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
 * Tabla del sistema: búsqueda, filtros, ordenación, paginación, estado de
 * carga, estado vacío y adaptación a móvil en un solo componente.
 *
 * **La paginación y la ordenación las resuelve el backend.** Aquí solo se
 * dibujan los controles y se avisa del cambio con `onCambiarPagina`,
 * `onCambiarTamano` y `onCambiarOrden`; nunca se trocea ni se reordena en
 * memoria, porque `filas` contiene únicamente la página actual y no el conjunto
 * completo. Ordenar lo que se tiene a mano daría un orden falso: el primer
 * registro de la página no es el primero del listado.
 *
 * Bajo el punto de corte `md` la tabla se dibuja como una lista de tarjetas:
 * una fila de ocho columnas resulta ilegible en un teléfono, incluso con scroll
 * horizontal.
 *
 * Contrato de una columna:
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
 *  - `id`         clave del dato en la fila y campo que se manda al backend al
 *                 ordenar. Es también la clave de React de la celda.
 *  - `etiqueta`   texto del encabezado y del par etiqueta/valor en tarjeta.
 *  - `render`     dibuja la celda a partir de la fila completa; sin él se
 *                 muestra `fila[id]`, o un guion largo si no hay valor.
 *  - `ordenable`  habilita el clic en el encabezado. Requiere además que la
 *                 pantalla pase `onCambiarOrden`.
 *  - `sinOrden`   anula la ordenación aunque `ordenable` sea cierto; es lo que
 *                 lleva la columna de acciones, que no corresponde a un campo.
 *  - `principal`  marca la columna que hace de título de la tarjeta en móvil.
 *                 Si ninguna la lleva, se usa la primera.
 *  - `oculta`     la pantalla descarta la columna, normalmente según el rol.
 *  - `ancho`, `alineacion` y `sx` ajustan la presentación de la celda.
 *
 * La columna cuyo `id` es `acciones` recibe un trato aparte en la vista de
 * tarjetas: se separa del resto y se dibuja al pie, bajo una línea divisoria.
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

    // Columnas que el usuario apagó desde el menú, y ancla de ese menú.
    const [ocultas, setOcultas] = useState([]);
    const [anclaColumnas, setAnclaColumnas] = useState(null);

    // El filtrado va en dos pasos. Primero caen las columnas que la pantalla
    // marca como `oculta` —normalmente por rol—, que no deben aparecer siquiera
    // en el menú de columnas; luego, sobre las que quedan, las que el usuario
    // decidió esconder.
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

    // Un filtro cuenta como activo cuando su valor difiere del predeterminado;
    // es lo que alimenta el distintivo con el número de filtros aplicados.
    const filtrosActivos = filtros.filter((f) => f.valor && f.valor !== f.valorPorDefecto).length;

    const alternarColumna = (id) => {
        setOcultas((prev) => (prev.includes(id) ? prev.filter((c) => c !== id) : [...prev, id]));
    };

    // Clicar la columna ya activa invierte el sentido; clicar otra empieza de
    // nuevo en ascendente.
    const alOrdenar = (campo) => {
        if (!onCambiarOrden) return;
        const esMismo = orden?.campo === campo;
        const direccion = esMismo && orden?.direccion === 'asc' ? 'desc' : 'asc';
        onCambiarOrden(campo, direccion);
    };

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
                        // Crece con la barra pero con tope: un buscador a todo
                        // lo ancho desequilibra la fila en pantallas grandes.
                        flexGrow: 1,
                        minWidth: { xs: '100%', sm: 240 },
                        maxWidth: { sm: 360 },
                        bgcolor: 'background.paper',
                    }}
                    // Los adornos del campo se declaran en `slotProps.input`,
                    // que es la vía de MUI 9 para llegar al InputBase interno.
                    slotProps={{
                        input: {
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
                        },
                    }}
                />
            )}

            {/* Cada filtro fija su propio ancho con `ancho`, porque lo que
                muestran no ocupa lo mismo: un estado necesita menos espacio
                que un nombre de área. */}
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

            {/* El selector de columnas solo aparece cuando hay bastantes como
                para estorbar, y nunca en móvil: allí las filas ya se dibujan
                como tarjetas y no hay columnas que esconder. */}
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
                    // Quien recibe el aviso vuelve a la primera página: la
                    // página 7 de un listado de 10 en 10 puede no existir
                    // cuando se pasa a 50 filas.
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

    // Vista de tarjetas. La columna principal encabeza cada tarjeta, la de
    // acciones baja al pie y el resto se dibuja como pares etiqueta/valor.
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

                {/* Sin opciones de tamaño: el selector de filas por página
                    ocupa más de lo que aporta en una pantalla estrecha, pero
                    los botones de avance sí se conservan. */}
                {controlesPaginacion([])}
            </Paper>
        );
    }

    return (
        <Paper variant="outlined" sx={{ overflow: 'hidden' }}>
            {barraHerramientas}

            {/* El scroll horizontal vive DENTRO del contenedor: la página nunca
                se desplaza en horizontal, solo la tabla. `anchoMinimo` fuerza
                ese scroll cuando las columnas no caben. */}
            <TableContainer sx={{ maxWidth: '100%' }}>
                <Table sx={{ minWidth: sinDatos ? 'auto' : anchoMinimo }} aria-busy={cargando}>
                    <TableHead>
                        {/* El encabezado es inerte al cursor: ni cambia de
                            color ni de fondo, ni anima nada al pasar por
                            encima. Las reglas se repiten aquí aunque el tema ya
                            las declare, porque `sx` genera una clase propia con
                            más especificidad que el selector global y sin ellas
                            el estilo del tema quedaría anulado.

                            La única flecha visible es la de la columna por la
                            que se ordena (`.Mui-active`): informa del orden
                            vigente, no reacciona al ratón. */}
                        <TableRow
                            sx={{
                                bgcolor: 'primary.main',
                                transition: 'none',
                                '&:hover': { bgcolor: 'primary.main' },
                                // La celda solo anula la transición y no toca
                                // el fondo: el guinda lo pinta la fila y la
                                // celda lo deja ver. Fijarla en `transparent`
                                // taparía ese fondo en lugar de conservarlo.
                                '& .MuiTableCell-head': {
                                    transition: 'none',
                                },
                                '& .MuiTableSortLabel-root': {
                                    transition: 'none',
                                    '&:hover': { bgcolor: 'transparent', color: 'inherit' },
                                },
                                // La flecha arranca invisible y solo se muestra
                                // en la columna activa; en las demás tampoco
                                // asoma al pasar el cursor.
                                '& .MuiTableSortLabel-icon': {
                                    opacity: 0,
                                    transition: 'none',
                                },
                                '& .MuiTableSortLabel-root:hover .MuiTableSortLabel-icon': {
                                    opacity: 0,
                                },
                                '& .MuiTableSortLabel-root.Mui-active .MuiTableSortLabel-icon': {
                                    opacity: 1,
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
