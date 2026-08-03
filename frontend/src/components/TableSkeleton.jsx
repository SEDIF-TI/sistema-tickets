import { Skeleton, TableBody, TableCell, TableRow } from '@mui/material';

/**
 * Esqueleto de carga para tablas.
 *
 * Reserva el espacio que ocupará el contenido real, de modo que la página no
 * salta cuando llegan los datos. Es preferible a un spinner centrado: comunica
 * la forma de lo que está por venir.
 *
 * @param {number} columnas  Número de columnas de la tabla.
 * @param {number} filas     Filas de relleno mientras carga.
 */
export default function TableSkeleton({ columnas = 5, filas = 5 }) {
    return (
        // aria-hidden: es una animación de espera, no información. El estado de
        // carga se anuncia con aria-busy en el contenedor de la tabla.
        <TableBody aria-hidden="true">
            {Array.from({ length: filas }).map((_, fila) => (
                <TableRow key={fila}>
                    {Array.from({ length: columnas }).map((__, columna) => (
                        <TableCell key={columna}>
                            <Skeleton
                                variant="text"
                                // Anchos desiguales: imita texto real y evita
                                // el aspecto de rejilla artificial.
                                width={columna === 0 ? '40%' : `${65 + ((columna * 7) % 30)}%`}
                                height={22}
                            />
                        </TableCell>
                    ))}
                </TableRow>
            ))}
        </TableBody>
    );
}
