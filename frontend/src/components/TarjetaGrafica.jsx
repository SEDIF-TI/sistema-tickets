import { useState } from 'react';
import {
    Paper, Box, Typography, ToggleButtonGroup, ToggleButton, Table, TableBody,
    TableCell, TableHead, TableRow, TableContainer
} from '@mui/material';
import BarChartIcon from '@mui/icons-material/BarChart';
import TableRowsIcon from '@mui/icons-material/TableRows';

import EmptyState from './EmptyState';

/**
 * Contenedor de una gráfica del panel. La gráfica en sí llega como `children`;
 * esta tarjeta aporta el encabezado, el alto y las dos formas de leer el dato.
 *
 * Tres detalles sostienen el componente:
 *
 *  1. **Sin datos no se monta la gráfica.** El `ResponsiveContainer` de
 *     Recharts mide el alto de su padre, y con una lista vacía el contenido
 *     colapsa: la medición da negativa, la biblioteca protesta en consola y
 *     queda un hueco en blanco. En su lugar se muestra el estado vacío, que
 *     además explica por qué no hay nada.
 *
 *  2. **Toda gráfica tiene su tabla.** El interruptor del encabezado alterna
 *     entre ambas vistas. Quien use lector de pantalla, o no distinga los
 *     colores, llega igualmente a las cifras; el color nunca es el único
 *     portador de la información.
 *
 *  3. **El alto se reserva aquí**, incluyendo la banda de las etiquetas del
 *     eje, para que el contenedor tenga una altura real que medir y la tarjeta
 *     no acabe con un scroll interno diminuto.
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
                    // El alto se fija en esta caja y no en el
                    // ResponsiveContainer, que solo sabe medir el de su padre.
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
                                                // Dígitos de ancho fijo solo en
                                                // las columnas numéricas: las
                                                // cifras quedan alineadas y se
                                                // pueden comparar de un vistazo.
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
