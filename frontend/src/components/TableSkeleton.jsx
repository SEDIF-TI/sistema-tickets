import { Skeleton, TableBody, TableCell, TableRow } from '@mui/material';

/**
 * Esqueleto de carga para tablas. Lo monta `DynamicTable` en lugar del cuerpo
 * mientras la petición está en curso.
 *
 * Reserva el espacio que ocupará el contenido real, de modo que la página no dé
 * un salto cuando lleguen los datos, y anticipa la forma de lo que viene: un
 * spinner centrado no hace ninguna de las dos cosas.
 *
 * @param {number} columnas  Número de columnas de la tabla.
 * @param {number} filas     Filas de relleno mientras carga.
 */
export default function TableSkeleton({ columnas = 5, filas = 5 }) {
    return (
        // Se oculta a los lectores de pantalla porque es una animación de
        // espera y no información; del estado de carga ya avisa el `aria-busy`
        // que DynamicTable pone en la tabla.
        <TableBody aria-hidden="true">
            {Array.from({ length: filas }).map((_, fila) => (
                <TableRow key={fila}>
                    {Array.from({ length: columnas }).map((__, columna) => (
                        <TableCell key={columna}>
                            <Skeleton
                                variant="text"
                                // Anchos desiguales derivados del índice: se
                                // parecen a texto real en lugar de a una
                                // rejilla de bloques idénticos.
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
