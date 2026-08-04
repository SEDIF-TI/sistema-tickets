import { useState } from 'react';
import {
    Paper, Box, Typography, ToggleButtonGroup, ToggleButton, Table, TableBody,
    TableCell, TableHead, TableRow, TableContainer
} from '@mui/material';
import BarChartIcon from '@mui/icons-material/BarChart';
import TableRowsIcon from '@mui/icons-material/TableRows';

import EmptyState from './EmptyState';

/**
 * Contenedor de una gráfica del panel.
 *
 * Resuelve tres cosas que el panel anterior no cubría:
 *
 *  1. **El estado vacío.** `ResponsiveContainer` mide el alto de su padre; con
 *     una lista vacía el contenido colapsaba y Recharts avisaba en consola
 *     ("The width(-1) and height(-1) of chart should be greater than 0"),
 *     dejando un hueco en blanco sin explicación. Aquí, sin datos no se monta
 *     la gráfica: se muestra un mensaje que dice por qué está vacía.
 *
 *  2. **La vista de tabla.** Una gráfica no puede ser el único camino al dato:
 *     quien use lector de pantalla, o no distinga los colores, necesita las
 *     cifras. El botón alterna entre ambas y la tabla es el respaldo exigido
 *     cuando el color no alcanza el contraste mínimo.
 *
 *  3. **El alto reservado.** El contenedor fija su altura incluyendo la banda
 *     de las etiquetas del eje, de modo que la tarjeta no genera un scroll
 *     interno diminuto.
 *
 * @param {string} titulo  Qué mide la gráfica.
 * @param {React.ElementType} icono  Icono decorativo del encabezado.
 * @param {Array} datos  Filas; si viene vacío se muestra el estado vacío.
 * @param {number} alto  Alto del área de dibujo, etiquetas incluidas.
 * @param {string} vacio  Texto del estado vacío.
 * @param {Array} columnas  `[{ id, etiqueta }]` de la vista de tabla.
 */
export default function TarjetaGrafica({
    titulo,
    icono: Icono,
    datos = [],
    alto = 320,
    vacio = 'Todavía no hay datos suficientes para esta gráfica.',
    columnas = [{ id: 'nombre', etiqueta: 'Concepto' }, { id: 'cantidad', etiqueta: 'Total' }],
    children,
}) {
    const [vista, setVista] = useState('grafica');
    const hayDatos = Array.isArray(datos) && datos.length > 0;

    return (
        <Paper variant="outlined" sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
            <Box
                sx={{
                    px: 2,
                    py: 1.5,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    gap: 1,
                    borderBottom: '1px solid',
                    borderColor: 'divider',
                }}
            >
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, minWidth: 0 }}>
                    {Icono && <Icono fontSize="small" color="primary" aria-hidden="true" />}
                    <Typography variant="subtitle2" sx={{ fontWeight: 600 }} noWrap>
                        {titulo}
                    </Typography>
                </Box>

                {hayDatos && (
                    <ToggleButtonGroup
                        size="small"
                        exclusive
                        value={vista}
                        onChange={(_e, valor) => valor && setVista(valor)}
                        aria-label={`Forma de ver ${titulo}`}
                    >
                        <ToggleButton value="grafica" aria-label="Ver como gráfica">
                            <BarChartIcon fontSize="small" />
                        </ToggleButton>
                        <ToggleButton value="tabla" aria-label="Ver como tabla de datos">
                            <TableRowsIcon fontSize="small" />
                        </ToggleButton>
                    </ToggleButtonGroup>
                )}
            </Box>

            <Box sx={{ p: 2, flexGrow: 1, minWidth: 0 }}>
                {!hayDatos ? (
                    <EmptyState
                        icono={Icono}
                        titulo="Sin datos"
                        descripcion={vacio}
                        sx={{ py: 4 }}
                    />
                ) : vista === 'grafica' ? (
                    // El alto va aquí y no dentro de ResponsiveContainer: así
                    // el contenedor tiene una altura real que medir.
                    <Box sx={{ width: '100%', height: alto }}>{children}</Box>
                ) : (
                    <TableContainer sx={{ maxHeight: alto }}>
                        <Table size="small" stickyHeader>
                            <TableHead>
                                <TableRow>
                                    {columnas.map((c) => (
                                        <TableCell
                                            key={c.id}
                                            align={c.id === 'nombre' || c.id === 'fecha' ? 'left' : 'right'}
                                            sx={{ fontWeight: 600 }}
                                        >
                                            {c.etiqueta}
                                        </TableCell>
                                    ))}
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {datos.map((fila, indice) => (
                                    <TableRow key={fila.nombre ?? fila.fecha ?? indice} hover>
                                        {columnas.map((c) => (
                                            <TableCell
                                                key={c.id}
                                                align={c.id === 'nombre' || c.id === 'fecha' ? 'left' : 'right'}
                                                // Cifras alineadas en columna:
                                                // aquí sí conviene ancho fijo.
                                                sx={c.id === 'nombre' || c.id === 'fecha'
                                                    ? undefined
                                                    : { fontVariantNumeric: 'tabular-nums' }}
                                            >
                                                {fila[c.id] ?? '—'}
                                            </TableCell>
                                        ))}
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </TableContainer>
                )}
            </Box>
        </Paper>
    );
}
