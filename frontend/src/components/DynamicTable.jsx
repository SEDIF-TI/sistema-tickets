import {
    Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
    Typography, Box, TablePagination
} from '@mui/material';
import TableSkeleton from './TableSkeleton';
import EmptyState from './EmptyState';

/**
 * Tabla reutilizable del sistema.
 *
 * Antes cada pantalla reimplementaba su propia tabla: cabeceras con el color
 * escrito a mano, anchos distintos, y ninguna con estado de carga ni scroll
 * horizontal en móvil. Esta componente resuelve los tres casos (cargando,
 * vacía, con datos) de forma coherente.
 *
 * Las columnas se declaran como datos:
 *
 *   const columnas = [
 *     { id: 'id',      etiqueta: 'ID',     ancho: '80px' },
 *     { id: 'titulo',  etiqueta: 'Título' },
 *     { id: 'estatus', etiqueta: 'Estado', alineacion: 'center',
 *       render: (fila) => <Chip label={fila.estatus} /> },
 *   ];
 *
 * @param {Array}  columnas       Definición de columnas (ver arriba).
 * @param {Array}  filas          Datos a mostrar.
 * @param {boolean} cargando      Muestra el esqueleto de carga.
 * @param {string} claveFila      Campo identificador único (por defecto 'id').
 * @param {number} anchoMinimo    Ancho a partir del cual aparece scroll-x.
 * @param {object} vacio          Props para EmptyState cuando no hay datos.
 */
export default function DynamicTable({
    columnas = [],
    filas = [],
    cargando = false,
    claveFila = 'id',
    anchoMinimo = 800,
    vacio = {},
    onClickFila,
    // --- Paginación (opcional) ---
    // Se activa pasando `paginacion`. El troceado ocurre en el backend: aquí
    // solo se muestran los controles y se avisa del cambio de página.
    paginacion = null,
    onCambiarPagina,
    onCambiarTamano,
}) {
    const sinDatos = !cargando && filas.length === 0;
    const hayPaginacion = Boolean(paginacion) && !sinDatos;

    return (
        <Paper variant="outlined" sx={{ overflow: 'hidden' }}>
            {/* El scroll horizontal vive DENTRO del contenedor: la página nunca
                se desplaza en horizontal, solo la tabla. Es lo que hace usable
                una tabla de 8 columnas en una pantalla de 375px. */}
            <TableContainer sx={{ maxWidth: '100%' }}>
                <Table
                    sx={{ minWidth: sinDatos ? 'auto' : anchoMinimo }}
                    aria-busy={cargando}
                >
                    <TableHead>
                        <TableRow sx={{ bgcolor: 'primary.main' }}>
                            {columnas.map((col) => (
                                <TableCell
                                    key={col.id}
                                    align={col.alineacion || 'left'}
                                    sx={{
                                        color: 'primary.contrastText',
                                        width: col.ancho,
                                        // El hover de fila no debe teñir la cabecera.
                                        borderBottom: 'none',
                                    }}
                                >
                                    {col.etiqueta}
                                </TableCell>
                            ))}
                        </TableRow>
                    </TableHead>

                    {cargando ? (
                        <TableSkeleton columnas={columnas.length} filas={5} />
                    ) : (
                        <TableBody>
                            {sinDatos ? (
                                <TableRow sx={{ '&:hover': { bgcolor: 'transparent' } }}>
                                    <TableCell colSpan={columnas.length} sx={{ borderBottom: 'none', p: 0 }}>
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
                                        {columnas.map((col) => (
                                            <TableCell
                                                key={col.id}
                                                align={col.alineacion || 'left'}
                                                sx={col.sx}
                                            >
                                                {/* Si la columna trae `render`, manda esa función;
                                                    si no, se pinta el valor tal cual. */}
                                                {col.render
                                                    ? col.render(fila)
                                                    : (
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

            {/* Pista visual de que la tabla se desplaza, solo en pantallas
                donde el contenido no cabe. */}
            {!sinDatos && !cargando && (
                <Box
                    sx={{
                        display: { xs: 'block', md: 'none' },
                        px: 2, py: 1,
                        borderTop: '1px solid',
                        borderColor: 'divider',
                        bgcolor: 'grey.50',
                    }}
                >
                    <Typography variant="caption" color="text.secondary">
                        Desliza horizontalmente para ver todas las columnas
                    </Typography>
                </Box>
            )}

            {/* Controles de paginación. El backend devuelve el total real de
                registros, así que los números reflejan el conjunto completo y
                no solo lo que hay cargado en pantalla. */}
            {hayPaginacion && (
                <TablePagination
                    component="div"
                    count={paginacion.totalItems ?? 0}
                    page={paginacion.pagina ?? 0}
                    rowsPerPage={paginacion.tamano ?? 10}
                    rowsPerPageOptions={[10, 25, 50]}
                    onPageChange={(_evento, nuevaPagina) => onCambiarPagina?.(nuevaPagina)}
                    onRowsPerPageChange={(evento) => {
                        // Al cambiar el tamaño se vuelve a la primera página:
                        // la página 7 de un listado de 10 puede no existir con 50.
                        onCambiarTamano?.(parseInt(evento.target.value, 10));
                    }}
                    labelRowsPerPage="Filas por página"
                    labelDisplayedRows={({ from, to, count }) =>
                        `${from}–${to} de ${count !== -1 ? count : `más de ${to}`}`
                    }
                    getItemAriaLabel={(tipo) =>
                        tipo === 'previous' ? 'Página anterior' : 'Página siguiente'
                    }
                    sx={{ borderTop: '1px solid', borderColor: 'divider' }}
                />
            )}
        </Paper>
    );
}
